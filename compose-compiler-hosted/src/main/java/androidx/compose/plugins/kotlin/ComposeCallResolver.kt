/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.extensions.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectVariables
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext


class ComposeCallResolver(
    private val callResolver: CallResolver,
    private val project: Project,
    private val psiFactory: KtPsiFactory
) {

    @Suppress("UNUSED_PARAMETER")
    fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        if (candidates.isEmpty()) return candidates

        val composables = mutableListOf<FunctionDescriptor>()
        val nonComposables = mutableListOf<FunctionDescriptor>()
        val constructors = mutableListOf<ConstructorDescriptor>()

        var needToLookupComposer = false

        for (candidate in candidates) {
            if (candidate.hasComposableAnnotation()) {
                needToLookupComposer = true
                composables.add(candidate)
            } else {
                nonComposables.add(candidate)
            }
            if (candidate is ConstructorDescriptor) {
                needToLookupComposer = true
                constructors.add(candidate)
            }
        }

        // If none of the candidates are composable or constructors, then it's unnecessary for us
        // to do any work at all, since it will never be anything we intercept
        if (!needToLookupComposer) return candidates

        // TODO(lmr): refactor/removal of ktxcallresolver so we don't need to do this!!!
        // THREAD LOCAL!!!!
        if (KtxCallResolver.resolving.get().get()) return candidates

        // use the scope tower to find any variable that would resolve with "composer" in scope.
        val composer = scopeTower
            .lexicalScope
            .collectVariables(KtxNameConventions.COMPOSER, location)
            .firstOrNull()

        // If there is no composer in scope, then we cannot intercept. This means that we need to
        // remove any @Composable from the candidates
        if (composer == null) {
            return nonComposables
        }

        // TODO(lmr): figure out if we need to do something here
        // We might decide there are some composers that are not "valid", ie, I shouldn't be able
        // to call a composable if I just have `val composer = Unit` in scope... but with this
        // logic, you'd be able to. This will get refined as we pass a composer as a variable.
        val isValidComposer = true

        // If there are no constructors, then all of the candidates are either composables or
        // non-composable functions, and we follow normal resolution rules.
        if (isValidComposer && constructors.isEmpty()) {
            // we wrap the composable descriptors into a ComposableFunctionDescriptor so we know
            // to intercept it in the backend.
            return nonComposables + composables.map { ComposableFunctionDescriptor(it) }
        }

        // If we made it this far, we need to check and see if the constructors qualify as emit
        // calls instead of constructor calls.  First, we need to look at the composer to see
        // what kinds of "emittables" it accepts.
        // We cache the metadata into a writeable slice based on the descriptor
        val emitMetadata = ComposerEmitMetadata.getOrBuild(
            composer,
            callResolver,
            psiFactory,
            resolutionContext
        )

        val hasEmittableCandidate = constructors.any { emitMetadata.isEmittable(it.returnType) }

        // if none of the constructors are emittables, then all of the candidates are valid
        if (!hasEmittableCandidate) {
            return nonComposables + composables.map { ComposableFunctionDescriptor(it) }
        }

        // since some of the constructors are emittables, we fall back to resolving using the
        // ktx call resolver. This needs to be refactored to be simpler, but this should work as
        // a starting point.
        //
        // TODO(lmr): refactor this to remove KtxCallResolver and the use of the facade
        // THREAD LOCAL!!!!
        val facade = CallResolutionInterceptorExtension.facade.get().peek()
        val ktxCallResolver = KtxCallResolver(
            callResolver,
            facade,
            project,
            ComposableAnnotationChecker.get(project)
        )

        val context = ExpressionTypingContext.newContext(
            resolutionContext.trace,
            resolutionContext.scope,
            resolutionContext.dataFlowInfo,
            resolutionContext.expectedType,
            resolutionContext.languageVersionSettings,
            resolutionContext.dataFlowValueFactory
        )

        val call = resolutionContext.call

        val element = call.callElement as KtExpression

        val temporaryTraceForKtxCall =
            TemporaryTraceAndCache.create(
                context,
                "trace to resolve ktx call", element
            )

        val temporaryForKtxCall = context.replaceTraceAndCache(temporaryTraceForKtxCall)

        ktxCallResolver.initializeFromCall(call, temporaryForKtxCall)

        val resolvedKtxElementCall = ktxCallResolver.resolveFromCall(
            call,
            temporaryForKtxCall
        )

        val result = ComposableEmitDescriptor.fromKtxCall(
            resolvedKtxElementCall,
            scopeTower,
            name
        )

        if (result == null) {
            return nonComposables +
                    composables.map { ComposableFunctionDescriptor(it) } +
                    constructors.filter { !emitMetadata.isEmittable(it.returnType) }
        }

        // TODO(lmr): deal with this RESTART_CALLS_NEEDED stuff
        // Once we know we have a valid binding to a composable function call see if the scope need
        // the startRestartGroup and endRestartGroup information

        return listOf(result)
    }

}