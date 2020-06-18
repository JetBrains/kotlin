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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.extensions.internal.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionMutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope

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
            else -> descriptor
        }
    override fun getCandidateDescriptor(): T = composableCandidateDescriptor as T
    override fun getResultingDescriptor(): T {
        return when (val descriptor = underlying.resultingDescriptor) {
            is FunctionDescriptor -> ComposableFunctionDescriptor(descriptor)
            is PropertyDescriptor -> ComposablePropertyDescriptorImpl(descriptor)
            else -> descriptor
        } as T
    }
}

// TODO(lmr): This class is only needed for the syntax highlighting for composable calls right
//  now. The ComposableFunctionDescriptors are not otherwise needed. We should refactor the
//  syntax highlighting plugin ASAP so that we can get rid of this plugin.
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
        if (candidates.isEmpty()) return candidates

        val others = mutableListOf<Candidate>()
        val composables = mutableListOf<Candidate>()

        for (candidate in candidates) {
            val resolvedCall = candidate.resolvedCall
            val candidateDescriptor = resolvedCall.candidateDescriptor
            when {
                candidateDescriptor is ComposableFunctionDescriptor -> {
                    others.add(candidate)
                }
                resolvedCall is VariableAsFunctionResolvedCall &&
                resolvedCall.variableCall
                    .candidateDescriptor
                    .type
                    .hasComposableAnnotation() -> {
                    composables.add(candidate)
                }
                resolvedCall.candidateDescriptor.hasComposableAnnotation() -> {
                    composables.add(candidate)
                }
                else -> others.add(candidate)
            }
        }

        return when {
            composables.isEmpty() || !isInComposableScope(context) -> candidates
            else -> others + composables.map { ComposableCandidate(it) }
        }
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
        val others = mutableListOf<FunctionDescriptor>()
        val composables = mutableListOf<FunctionDescriptor>()

        for (candidate in candidates) {
            when {
                candidate.hasComposableAnnotation() -> {
                    composables.add(candidate)
                }
                else -> others.add(candidate)
            }
        }

        return when {
            composables.isEmpty() || !isInComposableScope(resolutionContext) -> candidates
            else -> others + composables.map { ComposableFunctionDescriptor(it) }
        }
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
}
