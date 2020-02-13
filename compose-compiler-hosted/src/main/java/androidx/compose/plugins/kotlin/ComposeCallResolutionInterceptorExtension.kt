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

import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.extensions.internal.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionMutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.KotlinType

typealias Candidate = NewResolutionOldInference.MyCandidate

fun ComposableCandidate(candidate: Candidate): Candidate {
    val (eagerDiagnostics, resolvedCall) = candidate

    if (resolvedCall !is VariableAsFunctionMutableResolvedCall) {
        @Suppress("UNCHECKED_CAST")
        return Candidate(
            eagerDiagnostics = eagerDiagnostics,
            resolvedCall = ComposableResolvedCall(resolvedCall),
            finalDiagnosticsComputation = null
        )
    }

    val functionCall = ComposableResolvedCall(resolvedCall.functionCall)

    val variableCall = resolvedCall.variableCall

    val newCall = VariableAsFunctionResolvedCallImpl(functionCall, variableCall)

    return Candidate(
        eagerDiagnostics = eagerDiagnostics,
        resolvedCall = newCall,
        finalDiagnosticsComputation = null
    )
}

@Suppress("UNCHECKED_CAST")
class ComposableResolvedCall<T : CallableDescriptor>(
    private val underlying: MutableResolvedCall<T>
) : MutableResolvedCall<T> by underlying {
    private val composableCandidateDescriptor =
        when (val descriptor = underlying.candidateDescriptor) {
            is FunctionDescriptor -> ComposableFunctionDescriptor(descriptor)
            is PropertyDescriptor -> ComposablePropertyDescriptorImpl(descriptor)
            else -> error("Expected FunctionDescriptor or PropertyDescriptor, found $descriptor")
        }
    override fun getCandidateDescriptor(): T = composableCandidateDescriptor as T
    override fun getResultingDescriptor(): T {
        return when (val descriptor = underlying.resultingDescriptor) {
            is FunctionDescriptor -> ComposableFunctionDescriptor(descriptor)
            is PropertyDescriptor -> ComposablePropertyDescriptorImpl(descriptor)
            else -> error("Expected FunctionDescriptor or PropertyDescriptor, found $descriptor")
        } as T
    }
}

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
open class ComposeCallResolutionInterceptorExtension : CallResolutionInterceptorExtension {
    override fun interceptCandidates(
        candidates: Collection<Candidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        callResolver: CallResolver?,
        name: Name,
        kind: NewResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ): Collection<Candidate> {
        if (callResolver == null) throw IllegalArgumentException("Call resolver must be non-null")

        if (candidates.isEmpty()) return candidates
        val bindingContext = context.trace.bindingContext
        val call = context.call
        val shouldIgnore = bindingContext[
                ComposeWritableSlices.IGNORE_COMPOSABLE_INTERCEPTION,
                call
        ] ?: false

        if (shouldIgnore) return candidates

        val composables = mutableListOf<Candidate>()
        val nonComposablesNonConstructors = mutableListOf<Candidate>()
        val alreadyInterceptedCandidates = mutableListOf<Candidate>()
        val constructors = mutableListOf<Candidate>()

        var needToLookupComposer = false

        for (candidate in candidates) {
            val resolvedCall = candidate.resolvedCall
            val candidateDescriptor = resolvedCall.candidateDescriptor
            when {
                candidateDescriptor is ComposableFunctionDescriptor -> {
                    alreadyInterceptedCandidates.add(candidate)
                }
                candidateDescriptor is ComposableEmitDescriptor -> {
                    alreadyInterceptedCandidates.add(candidate)
                }
                resolvedCall is VariableAsFunctionResolvedCall &&
                resolvedCall.variableCall
                    .candidateDescriptor
                    .type
                    .hasComposableAnnotation() -> {
                    needToLookupComposer = true
                    composables.add(candidate)
                }
                resolvedCall.candidateDescriptor.hasComposableAnnotation() -> {
                    needToLookupComposer = true
                    composables.add(candidate)
                }
                resolvedCall.candidateDescriptor is ConstructorDescriptor -> {
                    needToLookupComposer = true
                    constructors.add(candidate)
                }
                else -> nonComposablesNonConstructors.add(candidate)
            }
        }

        // If none of the candidates are composable or constructors, then it's unnecessary for us
        // to do any work at all, since it will never be anything we intercept
        if (!needToLookupComposer) return candidates

        if (!isInComposableScope(context)) return candidates

        // NOTE(lmr): I'm not implementing emittable interception here, since I believe it is not
        // needed. So in this case we just pass constructors right through, untouched.
        return nonComposablesNonConstructors +
                constructors +
                alreadyInterceptedCandidates +
                composables.map { ComposableCandidate(it) }
    }

    override fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver?,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        val element = resolutionContext.call.callElement as KtExpression
        val project = element.project
        if (callResolver == null) throw IllegalArgumentException("Call resolver must be non-null")

        if (candidates.isEmpty()) return candidates
        val bindingContext = resolutionContext.trace.bindingContext
        val call = resolutionContext.call
        val shouldIgnore = bindingContext[
                ComposeWritableSlices.IGNORE_COMPOSABLE_INTERCEPTION,
                call
        ] ?: false

        if (shouldIgnore) return candidates

        val composables = mutableListOf<FunctionDescriptor>()
        val nonComposablesNonConstructors = mutableListOf<FunctionDescriptor>()
        val constructors = mutableListOf<ConstructorDescriptor>()

        var needToLookupComposer = false

        for (candidate in candidates) {
            when {
                candidate.hasComposableAnnotation() -> {
                    needToLookupComposer = true
                    composables.add(candidate)
                }
                candidate is ConstructorDescriptor -> {
                    needToLookupComposer = true
                    constructors.add(candidate)
                }
                else -> nonComposablesNonConstructors.add(candidate)
            }
        }

        // If none of the candidates are composable or constructors, then it's unnecessary for us
        // to do any work at all, since it will never be anything we intercept
        if (!needToLookupComposer) return candidates

        // If there are no constructors, then all of the candidates are either composables or
        // non-composable functions, and we follow normal resolution rules.
        if (constructors.isEmpty()) {
            // we wrap the composable descriptors into a ComposableFunctionDescriptor so we know
            // to intercept it in the backend.
            return nonComposablesNonConstructors + composables.map {
                ComposableFunctionDescriptor(it)
            }
        }

        if (!isInComposableScope(resolutionContext)) return candidates

        val composerType = callResolver.findComposerCallAndDescriptor(resolutionContext)
            ?: return nonComposablesNonConstructors + constructors

        val psiFactory = KtPsiFactory(project, markGenerated = false)

        // If we made it this far, we need to check and see if the constructors qualify as emit
        // calls instead of constructor calls.  First, we need to look at the composer to see
        // what kinds of "emittables" it accepts.
        // We cache the metadata into a writeable slice based on the descriptor
        val composerMetadata = ComposerMetadata.getOrBuild(
            composerType,
            callResolver,
            psiFactory,
            resolutionContext
        )

        val emittables = constructors.filter {
            composerMetadata.isEmittable(it.returnType) && !it.returnType.isAbstract()
        }
        val hasEmittableCandidate = emittables.isNotEmpty()

        // if none of the constructors are emittables, then all of the candidates are valid
        if (!hasEmittableCandidate) {
            return nonComposablesNonConstructors + constructors + composables.map {
                ComposableFunctionDescriptor(it)
            }
        }

        // since some of the constructors are emittables, we fall back to resolving using the
        // emit resolver.
        val emitResolver = ComposeEmitResolver(
            callResolver,
            project,
            composerMetadata
        )

        val emitCandidates = emitResolver.resolveCandidates(
            call,
            emittables,
            name,
            resolutionContext
        )

        return nonComposablesNonConstructors +
                composables.map {
                    ComposableFunctionDescriptor(it)
                } +
                constructors.filter { !composerMetadata.isEmittable(it.returnType) } +
                emitCandidates
    }

    private fun CallResolver.findComposerCallAndDescriptor(
        context: BasicCallResolutionContext
    ): KotlinType? {
        // use the call resolver to find any variable that would resolve with "composer" in scope.
        return resolveComposer(context)?.resultingDescriptor?.returnType
            // If there is no composer in scope, then we decide to fall back to the ViewComposer
            // as a default. When we are properly inferring the composer type, this step should no
            // longer be needed. This provides a better developer experience currently though since
            // developers won't be required to import the composer into scope.
            ?: context
                .scope
                .ownerDescriptor
                .module
                .findClassAcrossModuleDependencies(ClassId.topLevel(ComposeFqNames.UiComposer))
                ?.defaultType
            ?: context
                .scope
                .ownerDescriptor
                .module
                .findClassAcrossModuleDependencies(ClassId.topLevel(ComposeFqNames.Composer))
                ?.defaultType
    }

    private fun isInComposableScope(resolutionContext: BasicCallResolutionContext): Boolean {
        val call = resolutionContext.call
        val temporaryTraceForComposeableCall =
            TemporaryTraceAndCache.create(
                resolutionContext,
                "trace to resolve composable call", call.callElement as KtExpression
            )

        val composableAnnotationChecker =
            ComposableAnnotationChecker.get(call.callElement.project)

        // Ensure we are in a composable context
        // TODO(lmr): there ought to be a better way to do this
        var walker: PsiElement? = call.callElement
        while (walker != null) {
            val descriptor = try {
                resolutionContext.trace[BindingContext.FUNCTION, walker]
            } catch (e: Exception) {
                null
            }
            if (descriptor != null) {
                val composability = composableAnnotationChecker.analyze(
                    temporaryTraceForComposeableCall.trace,
                    descriptor
                )
                if (composability != ComposableAnnotationChecker.Composability.NOT_COMPOSABLE) {
                    return true
                }

                // If the descriptor is for an inlined lambda, infer composability from the
                // outer scope
                if (!(walker is KtFunction) ||
                    !InlineUtil.isInlinedArgument(
                        walker,
                        resolutionContext.trace.bindingContext,
                        true
                    )
                )
                    break
            }
            walker = try { walker.parent } catch (e: Throwable) { null }
        }
        return false
    }

    private fun CallResolver.resolveVar(
        name: Name,
        context: BasicCallResolutionContext
    ): OverloadResolutionResults<CallableDescriptor> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context,
            "trace to resolve variable",
            context.call.callElement as KtExpression
        )
        val call = makeCall(context.call.callElement)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)

        )
        contextForVariable.trace.record(
            ComposeWritableSlices.IGNORE_COMPOSABLE_INTERCEPTION,
            call,
            true
        )
        return computeTasksAndResolveCall<CallableDescriptor>(
            contextForVariable,
            name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Variable
        )
    }

    private fun CallResolver.resolveComposer(context: BasicCallResolutionContext):
            ResolvedCall<CallableDescriptor>? {

        // The composer is currently resolved as whatever is currently in scope with the name "composer".
        val resolvedComposer = resolveVar(KtxNameConventions.COMPOSER, context)

        if (!resolvedComposer.isSuccess) {
            return null
        }

        return resolvedComposer.resultingCall
    }

    private fun KotlinType.isAbstract(): Boolean {
        val modality = (constructor.declarationDescriptor as? ClassDescriptor)?.modality
        return modality == Modality.ABSTRACT || modality == Modality.SEALED
    }
}
