/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.doNothing
import java.util.*

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
            context: ExpressionTypingContext,
            receiver: ReceiverValue?,
            name: Name,
            callElement: KtExpression?,
            reportErrorsOn: KtExpression? = callElement,
            callKind: FakeCallKind = FakeCallKind.OTHER,
            vararg argumentTypes: KotlinType
    ): OverloadResolutionResults<FunctionDescriptor> {
        val traceWithFakeArgumentInfo = TemporaryBindingTrace.create(context.trace, "trace to store fake argument for", name)
        val fakeArguments = ArrayList<KtExpression>()
        for (type in argumentTypes) {
            fakeArguments.add(ExpressionTypingUtils.createFakeExpressionOfType(project, traceWithFakeArgumentInfo,
                                                                               "fakeArgument" + fakeArguments.size, type))
        }
        return makeAndResolveFakeCall(receiver, context.replaceBindingTrace(traceWithFakeArgumentInfo),
                                      fakeArguments, name, callElement, callKind, reportErrorsOn).second
    }

    @JvmOverloads
    fun resolveFakeCall(
            context: ExpressionTypingContext,
            receiver: ReceiverValue,
            name: Name,
            callElement: KtExpression,
            reportErrorsOn: KtExpression = callElement,
            callKind: FakeCallKind = FakeCallKind.OTHER,
            valueArguments: List<KtExpression> = emptyList()
    ): OverloadResolutionResults<FunctionDescriptor> {
        return makeAndResolveFakeCall(receiver, context, valueArguments, name, callElement, callKind, reportErrorsOn).second
    }

    fun makeAndResolveFakeCall(
            receiver: ReceiverValue?,
            context: ExpressionTypingContext,
            valueArguments: List<KtExpression>,
            name: Name,
            callElement: KtExpression?,
            callKind: FakeCallKind = FakeCallKind.OTHER,
            reportErrorsOn: KtExpression? = callElement
    ): Pair<Call, OverloadResolutionResults<FunctionDescriptor>> {
        val fakeTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve fake call for", name)
        val fakeBindingTrace = context.replaceBindingTrace(fakeTrace)

        var hasUnreportedError = false
        val result = makeAndResolveFakeCallInContext(receiver, fakeBindingTrace, valueArguments, name, callElement) { fake ->
            fakeTrace.commit({ slice, diagnostic, key ->
                // excluding all entries related to fake expression
                // convert all errors on this expression to ITERATOR_MISSING on callElement
                val isFakeKey = key == fake
                if (diagnostic?.severity == Severity.ERROR && isFakeKey) {
                    hasUnreportedError = true
                }
                !isFakeKey
            }, true)
        }
        if (hasUnreportedError && reportErrorsOn != null) {
            when (callKind) {
                FakeCallKind.ITERATOR -> context.trace.report(Errors.ITERATOR_MISSING.on(reportErrorsOn))
                FakeCallKind.COMPONENT ->
                    if (receiver != null) {
                        context.trace.report(Errors.COMPONENT_FUNCTION_MISSING.on(reportErrorsOn, name, receiver.type))
                    }
                FakeCallKind.OTHER -> {}
            }
        }
        return result
    }

    @JvmOverloads fun makeAndResolveFakeCallInContext(
            receiver: ReceiverValue?,
            context: ExpressionTypingContext,
            valueArguments: List<KtExpression>,
            name: Name,
            callElement: KtExpression?,
            onSuccess: (KtSimpleNameExpression) -> Unit = doNothing()
    ): Pair<Call, OverloadResolutionResults<FunctionDescriptor>> {
        val fake = KtPsiFactory(project).createSimpleName(name.asString())
        val call = CallMaker.makeCallWithExpressions(callElement ?: fake, receiver, null, fake, valueArguments)
        val results = callResolver.resolveCallWithGivenName(context, call, fake, name)

        if (results.isSuccess) {
            onSuccess(fake)
        }

        return Pair(call, results)
    }
}
