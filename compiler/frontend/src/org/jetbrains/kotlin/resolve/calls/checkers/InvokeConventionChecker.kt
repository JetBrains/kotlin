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

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

class InvokeConventionChecker : CallChecker {
    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (resolvedCall is VariableAsFunctionResolvedCallImpl) {
            val functionCall = resolvedCall.functionCall
            val variableCall = resolvedCall.variableCall
            if (functionCall.dispatchReceiver.exists() && functionCall.extensionReceiver.exists() && KotlinBuiltIns.isExactExtensionFunctionType(variableCall.resultingDescriptor.type)) {
                if (variableCall.dispatchReceiver is ExpressionReceiver || variableCall.extensionReceiver is ExpressionReceiver) {
                    val callElement = variableCall.call.callElement
                    context.trace.report(Errors.INVOKE_ON_EXTENSION_FUNCTION_WITH_EXPLICIT_DISPATCH_RECEIVER.on(callElement))
                }
            }
        }
    }

}