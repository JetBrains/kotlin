/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types.expressions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.tasks.AbstractTracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType

enum class FakeCallKind {
    ITERATOR,
    COMPONENT,
    OTHER
}

class FakeCallResolver(
        private val project: Project,
        private val callResolver: CallResolver
) {
    fun resolveFakeCall(
            context: ResolutionContext<*>,
            receiver: ReceiverValue?,
            name: Name,
            callElement: KtExpression,
            reportErrorsOn: KtExpression,
            callKind: FakeCallKind,
            valueArguments: List<KtExpression>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val fakeTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve fake call for", name)
        val fakeBindingTrace = context.replaceBindingTrace(fakeTrace)

        var reportIsMissingError = false
        val realExpression = RealExpression(reportErrorsOn, callKind)
        val result = makeAndResolveFakeCallInContext(receiver, fakeBindingTrace, valueArguments, name, callElement, realExpression) { fake ->
            reportIsMissingError = fakeTrace.bindingContext.diagnostics.noSuppression().forElement(fake).any { it.severity == Severity.ERROR }
            fakeTrace.commit({ _, key -> key != fake }, true)
        }

        val resolutionResults = result.second
        if (reportIsMissingError) {
            val diagnostic = when (callKind) {
                FakeCallKind.ITERATOR -> Errors.ITERATOR_MISSING.on(reportErrorsOn)
                FakeCallKind.COMPONENT -> if (receiver != null) Errors.COMPONENT_FUNCTION_MISSING.on(reportErrorsOn, name, receiver.type) else null
                FakeCallKind.OTHER -> null
            }

            if (diagnostic != null) {
                context.trace.report(diagnostic)
            }
        }

        return resolutionResults
    }

    private class TracingStrategyForComponentCall(
            fakeExpression: KtReferenceExpression,
            val reportErrorsOn: KtExpression,
            val name: Name,
            val call: Call
    ) : TracingStrategy by TracingStrategyImpl.create(fakeExpression, call) {

        override fun <D : CallableDescriptor?> ambiguity(trace: BindingTrace, descriptors: Collection<ResolvedCall<D>>) {
            trace.report(Errors.COMPONENT_FUNCTION_AMBIGUITY.on(reportErrorsOn, name, descriptors))
        }

        override fun unsafeCall(trace: BindingTrace, type: KotlinType, isCallForImplicitInvoke: Boolean) {
            trace.report(Errors.COMPONENT_FUNCTION_ON_NULLABLE.on(reportErrorsOn, name))
        }

        override fun typeInferenceFailed(context: ResolutionContext<*>, inferenceErrorData: InferenceErrorData) {
            val diagnostic = AbstractTracingStrategy.typeInferenceFailedDiagnostic(context, inferenceErrorData, reportErrorsOn, call)
            if (diagnostic != null) {
                context.trace.report(diagnostic)
            }
        }
    }

    private class TracingStrategyForIteratorCall(
            fakeExpression: KtReferenceExpression,
            val reportErrorsOn: KtExpression,
            val call: Call
    ) : TracingStrategy by TracingStrategyImpl.create(fakeExpression, call) {

        override fun <D : CallableDescriptor?> ambiguity(trace: BindingTrace, descriptors: Collection<ResolvedCall<D>>) {
            trace.report(Errors.ITERATOR_AMBIGUITY.on(reportErrorsOn, descriptors))
        }

        override fun unsafeCall(trace: BindingTrace, type: KotlinType, isCallForImplicitInvoke: Boolean) {
            trace.report(Errors.ITERATOR_ON_NULLABLE.on(reportErrorsOn))
        }

        override fun typeInferenceFailed(context: ResolutionContext<*>, inferenceErrorData: InferenceErrorData) {
            val diagnostic = AbstractTracingStrategy.typeInferenceFailedDiagnostic(context, inferenceErrorData, reportErrorsOn, call)
            if (diagnostic != null) {
                context.trace.report(diagnostic)
            }
        }
    }

    @JvmOverloads
    fun makeAndResolveFakeCallInContext(
            receiver: ReceiverValue?,
            context: ResolutionContext<*>,
            valueArguments: List<KtExpression>,
            name: Name,
            callElement: KtExpression,
            realExpression: RealExpression? = null,
            onComplete: (KtSimpleNameExpression) -> Unit = { _ -> }
    ): Pair<Call, OverloadResolutionResults<FunctionDescriptor>> {
        val fakeCalleeExpression = KtPsiFactory(project, markGenerated = false).createSimpleName(name.asString())
        val call = CallMaker.makeCallWithExpressions(callElement, receiver, null, fakeCalleeExpression, valueArguments)

        val tracingStrategy = when (realExpression?.callKind) {
            FakeCallKind.ITERATOR -> TracingStrategyForIteratorCall(fakeCalleeExpression, realExpression.expressionToReportErrorsOn, call)
            FakeCallKind.COMPONENT -> TracingStrategyForComponentCall(fakeCalleeExpression, realExpression.expressionToReportErrorsOn, name, call)
            else -> null
        }

        val results = if (tracingStrategy != null)
            callResolver.resolveCallWithGivenName(context, call, name, tracingStrategy)
        else
            callResolver.resolveCallWithGivenName(context, call, fakeCalleeExpression, name)

        onComplete(fakeCalleeExpression)

        return Pair(call, results)
    }

    class RealExpression(val expressionToReportErrorsOn: KtExpression, val callKind: FakeCallKind)
}
