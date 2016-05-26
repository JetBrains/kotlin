/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.coroutines

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.expressions.FakeCallKind
import org.jetbrains.kotlin.types.expressions.FakeCallResolver
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * @returns type of first value parameter if function is 'operator handleResult' in coroutines controller
 */
fun SimpleFunctionDescriptor.getExpectedTypeForCoroutineControllerHandleResult(): KotlinType? {
    if (!isOperator || name != OperatorNameConventions.COROUTINE_HANDLE_RESULT) return null

    return valueParameters.getOrNull(0)?.type
}

val CallableDescriptor.controllerTypeIfCoroutine: KotlinType?
    get() {
        if (this !is AnonymousFunctionDescriptor || !this.isCoroutine) return null

        return this.extensionReceiverParameter?.returnType
    }

fun FakeCallResolver.resolveCoroutineHandleResultCallIfNeeded(
        callElement: KtExpression,
        expressionToReturn: KtExpression?,
        functionDescriptor: FunctionDescriptor,
        context: ResolutionContext<*>
) {
    functionDescriptor.controllerTypeIfCoroutine ?: return

    val info = if (expressionToReturn != null)
        context.trace.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, expressionToReturn)
    else
        null

    val temporaryBindingTrace = TemporaryBindingTrace.create(context.trace, "trace to store fake argument for", "continuation")

    val continuation =
            ExpressionTypingUtils.createFakeExpressionOfType(
                    callElement.project, temporaryBindingTrace, "continuation",
                    // should be Continuation<Nothing>
                    functionDescriptor.builtIns.nothingType)

    val firstArgument =
            if (expressionToReturn == null || info != null && info.type != null && KotlinBuiltIns.isUnit(info.type))
                ExpressionTypingUtils.createFakeExpressionOfType(
                        callElement.project, temporaryBindingTrace, "unit", functionDescriptor.builtIns.unitType)
            else expressionToReturn

    val resolutionResults = resolveFakeCall(
            context.replaceBindingTrace(temporaryBindingTrace), functionDescriptor.extensionReceiverParameter!!.value,
            OperatorNameConventions.COROUTINE_HANDLE_RESULT, callElement, callElement, FakeCallKind.OTHER,
            listOf(firstArgument, continuation))

    if (resolutionResults.isSuccess) {
        context.trace.record(BindingContext.RETURN_HANDLE_RESULT_RESOLVED_CALL, callElement, resolutionResults.resultingCall)
    }
}

fun KotlinType.isValidContinuation() =
        (constructor.declarationDescriptor as? ClassDescriptor)?.fqNameUnsafe == DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME.toUnsafe()
