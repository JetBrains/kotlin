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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

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

        var unreportedDiagnostic: Diagnostic? = null
        val result = makeAndResolveFakeCallInContext(receiver, fakeBindingTrace, valueArguments, name, callElement) { fake, isSuccess ->
            unreportedDiagnostic = fakeTrace.bindingContext.diagnostics.noSuppression().forElement(fake).firstOrNull { it.severity == Severity.ERROR }

            if (isSuccess) {
                fakeTrace.commit(
                        { _, key ->
                            // excluding all entries related to fake expression
                            // convert all errors on this expression to ITERATOR_MISSING on callElement
                            key != fake
                        }, true
                )
            }
        }

        val resolutionResults = result.second
        if (!resolutionResults.isSuccess || unreportedDiagnostic != null) {
            val isUnsafeCall = unreportedDiagnostic?.factory == Errors.UNSAFE_CALL
            val diagnostic = when (callKind) {
                FakeCallKind.ITERATOR ->
                    when {
                        resolutionResults.isAmbiguity -> Errors.ITERATOR_AMBIGUITY.on(reportErrorsOn, resolutionResults.resultingCalls)
                        isUnsafeCall                  -> Errors.ITERATOR_ON_NULLABLE.on(reportErrorsOn)
                        else                          -> Errors.ITERATOR_MISSING.on(reportErrorsOn)
                    }
                FakeCallKind.COMPONENT ->
                    when {
                        resolutionResults.isAmbiguity -> Errors.COMPONENT_FUNCTION_AMBIGUITY.on(
                                                            reportErrorsOn, name, resolutionResults.resultingCalls)
                        isUnsafeCall                  -> Errors.COMPONENT_FUNCTION_ON_NULLABLE.on(reportErrorsOn, name)
                        receiver != null              -> Errors.COMPONENT_FUNCTION_MISSING.on(reportErrorsOn, name, receiver.type)
                        else                          -> null
                    }
                FakeCallKind.OTHER -> null
            }

            if (diagnostic != null) {
                context.trace.report(diagnostic)
            }
        }

        return resolutionResults
    }

    @JvmOverloads
    fun makeAndResolveFakeCallInContext(
            receiver: ReceiverValue?,
            context: ResolutionContext<*>,
            valueArguments: List<KtExpression>,
            name: Name,
            callElement: KtExpression,
            onComplete: (KtSimpleNameExpression, Boolean) -> Unit = { _, _ -> }
    ): Pair<Call, OverloadResolutionResults<FunctionDescriptor>> {
        val fakeCalleeExpression = KtPsiFactory(project, markGenerated = false).createSimpleName(name.asString())
        val call = CallMaker.makeCallWithExpressions(
                callElement, receiver, /* callOperationNode = */ null, fakeCalleeExpression, valueArguments
        )
        val results = callResolver.resolveCallWithGivenName(context, call, fakeCalleeExpression, name)

        onComplete(fakeCalleeExpression, results.isSuccess)

        return Pair(call, results)
    }
}
