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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.coroutines.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.kotlin.resolve.BindingContext.TAIL_RECURSION_CALL

class TailRecursionCodegen(
    private val context: MethodContext,
    private val codegen: ExpressionCodegen,
    private val v: InstructionAdapter,
    private val state: GenerationState
) {

    fun isTailRecursion(resolvedCall: ResolvedCall<*>): Boolean {
        val status = state.bindingContext.get(TAIL_RECURSION_CALL, resolvedCall.call)
        return status != null && status.isDoGenerateTailRecursion
    }

    fun generateTailRecursion(resolvedCall: ResolvedCall<*>) {
        val fd = resolvedCall.resultingDescriptor.unwrapInitialDescriptorForSuspendFunction().let {
            it as? FunctionDescriptor
                ?: error("Resolved call doesn't refer to the function descriptor: $it")
        }
        val callable = codegen.resolveToCallable(fd, false, resolvedCall) as CallableMethod

        val arguments = resolvedCall.valueArgumentsByIndex ?: throw IllegalStateException("Failed to arrange value arguments by index: $fd")

        if (fd.isSuspend) {
            AsmUtil.pop(v, callable.getValueParameters().last().asmType)
        }

        assignParameterValues(fd, callable, arguments)
        if (callable.extensionReceiverType != null) {
            if (resolvedCall.extensionReceiver != fd.extensionReceiverParameter!!.value) {
                val expression = context.getReceiverExpression(codegen.typeMapper)
                expression.store(StackValue.onStack(callable.extensionReceiverType), v, true)
            } else {
                AsmUtil.pop(v, callable.extensionReceiverType)
            }
        }

        if (callable.dispatchReceiverType != null) {
            AsmUtil.pop(v, callable.dispatchReceiverType)
        }

        v.goTo(context.methodStartLabel)
    }

    private fun assignParameterValues(
        fd: CallableDescriptor,
        callableMethod: CallableMethod,
        valueArguments: List<ResolvedValueArgument>
    ) {
        val properDefaultInitialization =
            state.languageVersionSettings.supportsFeature(LanguageFeature.ProperComputationOrderOfTailrecDefaultParameters)

        val types = callableMethod.valueParameterTypes
        loop@ for (parameterDescriptor in fd.valueParameters.asReversed()) {
            val arg = valueArguments[parameterDescriptor.index]
            val type = types[parameterDescriptor.index]

            when (arg) {
                is ExpressionValueArgument -> {
                    val argumentExpression = arg.valueArgument?.getArgumentExpression()

                    if (argumentExpression is KtSimpleNameExpression) {
                        val resolvedCall = argumentExpression.getResolvedCall(state.bindingContext)
                        if (resolvedCall?.resultingDescriptor == parameterDescriptor.original) {
                            // do nothing: we shouldn't store argument to itself again
                            AsmUtil.pop(v, type)
                            continue@loop
                        }
                    }
                    //assign the parameter below
                }
                is DefaultValueArgument -> {
                    AsmUtil.pop(v, type)
                    if (properDefaultInitialization) {
                        //Initialization in proper order is performed in loop below
                        continue@loop
                    } else {
                        DefaultParameterValueLoader.DEFAULT.genValue(parameterDescriptor, codegen).put(type, v)
                    }
                }
                is VarargValueArgument -> {
                    // assign the parameter below
                }
                else -> throw UnsupportedOperationException("Unknown argument type: $arg in $fd")
            }

            store(parameterDescriptor, type)
        }

        if (properDefaultInitialization) {
            for (parameterDescriptor in fd.valueParameters) {
                val arg = valueArguments[parameterDescriptor.index]
                val type = types[parameterDescriptor.index]

                if (arg is DefaultValueArgument) {
                    DefaultParameterValueLoader.DEFAULT.genValue(parameterDescriptor, codegen).put(type, v)
                    store(parameterDescriptor, type)
                }
            }
        }
    }

    private fun store(parameterDescriptor: ValueParameterDescriptor, type: Type) {
        val index = getParameterVariableIndex(parameterDescriptor)
        v.store(index, type)
    }

    private fun getParameterVariableIndex(parameterDescriptor: ValueParameterDescriptor): Int {
        var index = codegen.lookupLocalIndex(parameterDescriptor)
        if (index == -1) {
            // in the case of a generic function recursively calling itself, the parameters on the call site are substituted
            index = codegen.lookupLocalIndex(parameterDescriptor.original)
        }

        if (index == -1) {
            throw IllegalStateException("Failed to obtain parameter index: $parameterDescriptor")
        }

        return index
    }
}
