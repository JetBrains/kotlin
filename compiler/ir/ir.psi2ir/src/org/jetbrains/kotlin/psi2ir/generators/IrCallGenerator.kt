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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize
import java.util.*

class IrCallGenerator(val irStatementGenerator: IrStatementGenerator) : IrGenerator {
    override val context: IrGeneratorContext get() = irStatementGenerator.context

    private val temporaries = newHashMapWithExpectedSize<KtExpression, VariableDescriptor>(3)

    fun putTemporary(ktExpression: KtExpression, temporaryVariableDescriptor: VariableDescriptor) {
        temporaries[ktExpression] = temporaryVariableDescriptor
    }

    private fun generateExpressionOrTemporary(ktExpression: KtExpression): IrExpression {
        val temporary = temporaries[ktExpression]
        return if (temporary != null)
            IrGetVariableExpressionImpl(ktExpression.startOffset, ktExpression.endOffset, getType(ktExpression), temporary)
        else
            irStatementGenerator.generateExpression(ktExpression)
    }

    fun generateCall(
            ktExpression: KtExpression,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ) = generateCall(ktExpression, getTypeOrFail(ktExpression), resolvedCall, operator, superQualifier)

    fun generateCall(
            ktExpression: KtExpression,
            resultType: KotlinType?,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ): IrExpression {
        val descriptor = resolvedCall.resultingDescriptor
        return when (descriptor) {
            is PropertyDescriptor ->
                IrGetPropertyExpressionImpl(
                        ktExpression.startOffset, ktExpression.endOffset, resultType,
                        resolvedCall.call.isSafeCall(), descriptor
                ).apply {
                    dispatchReceiver = generateReceiver(ktExpression, resolvedCall.dispatchReceiver)
                    extensionReceiver = generateReceiver(ktExpression, resolvedCall.extensionReceiver)
                }
            is FunctionDescriptor ->
                generateFunctionCall(descriptor, ktExpression, resultType, operator, resolvedCall, superQualifier)
            else ->
                TODO("Unexpected callable descriptor: $descriptor ${descriptor.javaClass.simpleName}")
        }
    }

    private fun ResolvedCall<*>.requiresArgumentReordering(): Boolean {
        var lastValueParameterIndex = -1
        for (valueArgument in call.valueArguments) {
            val argumentMapping = getArgumentMapping(valueArgument)
            if (argumentMapping !is ArgumentMatch || argumentMapping.isError()) {
                error("Value argument in function call is mapped with error")
            }
            val argumentIndex = argumentMapping.valueParameter.index
            if (argumentIndex < lastValueParameterIndex) return true
            lastValueParameterIndex = argumentIndex
        }
        return false
    }

    private fun generateFunctionCall(
            descriptor: FunctionDescriptor,
            ktExpression: KtExpression,
            resultType: KotlinType?,
            operator: IrOperator?,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            superQualifier: ClassDescriptor?
    ): IrExpression {
        val irCall = IrCallExpressionImpl(
                ktExpression.startOffset, ktExpression.endOffset, resultType,
                descriptor, resolvedCall.call.isSafeCall(), operator, superQualifier
        )
        irCall.dispatchReceiver = generateReceiver(ktExpression, resolvedCall.dispatchReceiver)
        irCall.extensionReceiver = generateReceiver(ktExpression, resolvedCall.extensionReceiver)

        return if (resolvedCall.requiresArgumentReordering()) {
            generateCallWithArgumentReordering(irCall, ktExpression, resolvedCall, resultType)
        }
        else {
            irCall.apply {
                val valueArguments = resolvedCall.valueArgumentsByIndex
                for (index in valueArguments!!.indices) {
                    val valueArgument = valueArguments[index]
                    val irArgument = generateValueArgument(valueArgument) ?: continue
                    irCall.putArgument(index, irArgument)
                }
            }
        }
    }

    private fun generateCallWithArgumentReordering(
            irCall: IrCallExpression,
            ktExpression: KtExpression,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            resultType: KotlinType?
    ): IrExpression {
        // TODO use IrLetExpression?

        val valueArgumentsInEvaluationOrder = resolvedCall.valueArguments.values

        val irBlock = IrBlockExpressionImpl(ktExpression.startOffset, ktExpression.endOffset, resultType,
                                            hasResult = isUsedAsExpression(ktExpression),
                                            isDesugared = true)

        val temporaryVariablesForValueArguments = HashMap<ResolvedValueArgument, Pair<VariableDescriptor, IrExpression>>()
        for (valueArgument in valueArgumentsInEvaluationOrder) {
            val irArgument = generateValueArgument(valueArgument) ?: continue
            val irTemporary = irStatementGenerator.declarationFactory.createTemporaryVariable(irArgument)
            irBlock.addStatement(irTemporary)

            temporaryVariablesForValueArguments[valueArgument] = Pair(irTemporary.descriptor, irArgument)
        }

        for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
            val (temporaryDescriptor, irArgument) = temporaryVariablesForValueArguments[valueArgument]!!
            val irGetTemporary = IrGetVariableExpressionImpl(irArgument.startOffset, irArgument.endOffset,
                                                             irArgument.type, temporaryDescriptor)
            irCall.putArgument(index, irGetTemporary)
        }

        irBlock.addStatement(irCall)

        return irBlock
    }

    // TODO smart casts on implicit receivers
    fun generateReceiver(ktExpression: KtExpression, receiver: ReceiverValue?): IrExpression? =
            when (receiver) {
                is ImplicitClassReceiver ->
                    IrThisExpressionImpl(ktExpression.startOffset, ktExpression.startOffset, receiver.type, receiver.classDescriptor)
                is ThisClassReceiver ->
                    (receiver as? ExpressionReceiver)?.expression?.let { receiverExpression ->
                        IrThisExpressionImpl(receiverExpression.startOffset, receiverExpression.endOffset, receiver.type, receiver.classDescriptor)
                    } ?: TODO("Non-implicit ThisClassReceiver should be an expression receiver")
                is ExpressionReceiver ->
                    generateExpressionOrTemporary(receiver.expression)
                is ClassValueReceiver ->
                    IrGetObjectValueExpressionImpl(receiver.expression.startOffset, receiver.expression.endOffset, receiver.type,
                                                   receiver.classQualifier.descriptor)
                is ExtensionReceiver ->
                    IrGetExtensionReceiverExpressionImpl(ktExpression.startOffset, ktExpression.startOffset, receiver.type,
                                                         receiver.declarationDescriptor.extensionReceiverParameter!!)
                null ->
                    null
                else ->
                    TODO("Receiver: ${receiver.javaClass.simpleName}")
            }

    fun generateValueArgument(valueArgument: ResolvedValueArgument): IrExpression? {
        when (valueArgument) {
            is DefaultValueArgument ->
                return null
            is ExpressionValueArgument ->
                return generateExpressionOrTemporary(valueArgument.valueArgument!!.getArgumentExpression()!!)
            is VarargValueArgument ->
                TODO("vararg")
            else ->
                TODO("Unexpected valueArgument: ${valueArgument.javaClass.simpleName}")
        }
    }
}
