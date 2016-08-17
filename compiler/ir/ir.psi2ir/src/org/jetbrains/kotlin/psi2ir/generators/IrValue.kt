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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.createDefaultGetExpression
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

interface IrValue {
    fun load(): IrExpression
}

class IrTemporaryVariableValue(val irVariable: IrVariable) : IrValue {
    override fun load(): IrExpression =
            irVariable.createDefaultGetExpression()
}

class IrSingleExpressionValue(val irExpression: IrExpression) : IrValue {
    override fun load() = irExpression
}

interface IrLValue : IrValue {
    fun store(irExpression: IrExpression): IrExpression
}

interface IrLValueWithAugmentedStore : IrLValue {
    fun augmentedStore(operatorCall: ResolvedCall<*>, irRhs: IrExpression): IrExpression
}

class IrVariableLValueValue(
        val ktElement: KtElement,
        val irOperator: IrOperator?,
        val descriptor: VariableDescriptor
) : IrLValue {
    override fun load(): IrExpression =
            IrGetVariableExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor, irOperator
            )

    override fun store(irExpression: IrExpression): IrExpression =
            IrSetVariableExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor, irExpression.toExpectedType(descriptor.type), irOperator
            )
}

class IrPropertyLValueValue(
        val ktElement: KtElement,
        val irOperator: IrOperator?,
        val descriptor: PropertyDescriptor,
        val dispatchReceiver: IrExpression?,
        val extensionReceiver: IrExpression?,
        val isSafe: Boolean
) : IrLValue {
    private fun IrPropertyAccessExpression.setReceivers() =
            apply {
                dispatchReceiver = this@IrPropertyLValueValue.dispatchReceiver
                extensionReceiver = this@IrPropertyLValueValue.extensionReceiver
            }

    override fun load(): IrExpression =
            IrGetPropertyExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor.type, isSafe, descriptor, irOperator
            ).setReceivers()

    override fun store(irExpression: IrExpression): IrExpression =
            IrSetPropertyExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    isSafe, descriptor, irExpression.toExpectedType(descriptor.type), irOperator
            ).setReceivers()
}

class IrIndexedLValue(
        var irStatementGenerator: IrStatementGenerator,
        val ktArrayAccessExpression: KtArrayAccessExpression,
        val irOperator: IrOperator?,
        val arrayValue: IrValue,
        val indexValues: List<Pair<KtExpression, IrValue>>,
        val indexedGetCall: ResolvedCall<*>?,
        val indexedSetCall: ResolvedCall<*>?
) : IrLValueWithAugmentedStore {
    override fun load(): IrExpression {
        if (indexedGetCall == null) throw AssertionError("Indexed LValue has no 'get' call: ${ktArrayAccessExpression.text}")

        val irBlock = IrBlockExpressionImpl(ktArrayAccessExpression.startOffset, ktArrayAccessExpression.endOffset,
                                            indexedGetCall.resultingDescriptor.returnType,
                                            hasResult = true, isDesugared = true)

        val callGenerator = IrCallGenerator(irStatementGenerator)

        defineContextVariables(irBlock, callGenerator)

        irBlock.addStatement(callGenerator.generateCall(ktArrayAccessExpression, indexedGetCall, irOperator))

        return irBlock
    }

    override fun store(irExpression: IrExpression): IrExpression {
        if (indexedSetCall == null) throw AssertionError("Indexed LValue has no 'set' call: ${ktArrayAccessExpression.text}")

        val irBlock = IrBlockExpressionImpl(ktArrayAccessExpression.startOffset, ktArrayAccessExpression.endOffset,
                                            indexedSetCall.resultingDescriptor.returnType,
                                            hasResult = true, isDesugared = true)

        val callGenerator = IrCallGenerator(irStatementGenerator)

        defineContextVariables(irBlock, callGenerator)

        callGenerator.putValue(indexedSetCall.resultingDescriptor.valueParameters.last(), IrSingleExpressionValue(irExpression))

        irBlock.addStatement(callGenerator.generateCall(ktArrayAccessExpression, indexedSetCall, irOperator))

        return irBlock
    }

    override fun augmentedStore(operatorCall: ResolvedCall<*>, irRhs: IrExpression): IrExpression {
        if (indexedGetCall == null) throw AssertionError("Indexed LValue has no 'get' call: ${ktArrayAccessExpression.text}")
        if (indexedSetCall == null) throw AssertionError("Indexed LValue has no 'set' call: ${ktArrayAccessExpression.text}")

        val irBlock = IrBlockExpressionImpl(ktArrayAccessExpression.startOffset, ktArrayAccessExpression.endOffset,
                                            indexedSetCall.resultingDescriptor.returnType,
                                            hasResult = true, isDesugared = true)

        val callGenerator = IrCallGenerator(irStatementGenerator)

        defineContextVariables(irBlock, callGenerator)

        callGenerator.putValue(operatorCall.resultingDescriptor.valueParameters[0], IrSingleExpressionValue(irRhs))

        val operatorCallReceiver = operatorCall.extensionReceiver ?: operatorCall.dispatchReceiver
        callGenerator.putValue(operatorCallReceiver!!,
                               IrSingleExpressionValue(callGenerator.generateCall(ktArrayAccessExpression, indexedGetCall, irOperator)))

        callGenerator.putValue(indexedSetCall.resultingDescriptor.valueParameters.last(),
                               IrSingleExpressionValue(callGenerator.generateCall(ktArrayAccessExpression, operatorCall, irOperator)))

        irBlock.addStatement(callGenerator.generateCall(ktArrayAccessExpression, indexedSetCall, irOperator))

        return irBlock
    }

    private fun defineContextVariables(irBlock: IrBlockExpression, callGenerator: IrCallGenerator) {
        irBlock.addStatement(callGenerator.createTemporary(ktArrayAccessExpression.arrayExpression!!, arrayValue.load()))

        for ((ktIndexExpression, irIndexValue) in indexValues) {
            irBlock.addStatement(callGenerator.createTemporary(ktIndexExpression, irIndexValue.load()))
        }
    }
}