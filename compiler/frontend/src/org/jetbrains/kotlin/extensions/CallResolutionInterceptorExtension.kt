package org.jetbrains.kotlin.extensions

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade
import java.util.*

interface CallResolutionInterceptorExtension {
    companion object : ProjectExtensionDescriptor<CallResolutionInterceptorExtension>(
        "org.jetbrains.kotlin.callResolutionInterceptorExtension",
        CallResolutionInterceptorExtension::class.java
    ) {
        val facade: ThreadLocal<Stack<ExpressionTypingFacade>> = object : ThreadLocal<Stack<ExpressionTypingFacade>>() {
            override fun initialValue() = Stack<ExpressionTypingFacade>()
        }
    }

    fun interceptCandidates(
        candidates: Collection<NewResolutionOldInference.MyCandidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        name: Name,
        kind: NewResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ): Collection<NewResolutionOldInference.MyCandidate> {
        return candidates
    }

    fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        return candidates
    }
}

