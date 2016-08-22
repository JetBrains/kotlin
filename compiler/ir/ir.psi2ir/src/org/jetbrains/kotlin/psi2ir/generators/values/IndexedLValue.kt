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

package org.jetbrains.kotlin.psi2ir.generators.values

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.defaultLoad
import org.jetbrains.kotlin.psi2ir.generators.CallGenerator
import org.jetbrains.kotlin.psi2ir.generators.Scope
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

class IndexedLValue(
        val statementGenerator: StatementGenerator,
        val ktArrayAccessExpression: KtArrayAccessExpression,
        val irOperator: IrOperator?,
        val irArray: IrExpression,
        override val type: KotlinType?,
        val indexValues: List<Pair<KtExpression, IrExpression>>,
        val indexedGetCall: ResolvedCall<*>?,
        val indexedSetCall: ResolvedCall<*>?
) : IrLValueWithAugmentedStore {
    override fun load(): IrExpression {
        if (indexedGetCall == null) throw AssertionError("Indexed LValue has no 'get' call: ${ktArrayAccessExpression.text}")

        return generateGetOrSetCallAsDesugaredBlock(indexedGetCall)
    }

    override fun store(irExpression: IrExpression): IrExpression {
        if (indexedSetCall == null) throw AssertionError("Indexed LValue has no 'set' call: ${ktArrayAccessExpression.text}")

        return generateGetOrSetCallAsDesugaredBlock(indexedSetCall)
    }

    private fun generateGetOrSetCallAsDesugaredBlock(call: ResolvedCall<*>, irArgument: IrExpression? = null): IrExpression {
        val callGenerator = CallGenerator(statementGenerator)
        setupCallGeneratorContext(callGenerator.scope)

        if (irArgument != null) {
            callGenerator.scope.putValue(call.resultingDescriptor.valueParameters.last(), SingleExpressionValue(irArgument))
        }

        return callGenerator.generateCall(ktArrayAccessExpression, call, irOperator)
    }

    override fun prefixAugmentedStore(operatorCall: ResolvedCall<*>, irOperator: IrOperator): IrExpression {
        if (indexedGetCall == null) throw AssertionError("Indexed LValue has no 'get' call: ${ktArrayAccessExpression.text}")
        if (indexedSetCall == null) throw AssertionError("Indexed LValue has no 'set' call: ${ktArrayAccessExpression.text}")

        val callGenerator = CallGenerator(statementGenerator)

        val irBlock = createDesugaredBlockWithTemporaries(indexedSetCall, callGenerator, true, irOperator)

        val operatorCallReceiver = operatorCall.extensionReceiver ?: operatorCall.dispatchReceiver
        val irGetCall = callGenerator.generateCall(ktArrayAccessExpression, indexedGetCall, irOperator)
        callGenerator.scope.putValue(operatorCallReceiver!!, SingleExpressionValue(irGetCall))

        val irOpCall = callGenerator.generateCall(ktArrayAccessExpression, operatorCall, irOperator)
        val irTmp = callGenerator.scope.createTemporaryVariable(irOpCall)
        irBlock.addStatement(irTmp)

        callGenerator.scope.putValue(indexedSetCall.resultingDescriptor.valueParameters.last(), VariableLValue(callGenerator, irTmp))

        irBlock.addStatement(callGenerator.generateCall(ktArrayAccessExpression, indexedSetCall, irOperator))

        irBlock.addStatement(irTmp.defaultLoad())

        return irBlock
    }

    override fun postfixAugmentedStore(operatorCall: ResolvedCall<*>, irOperator: IrOperator): IrExpression {
        if (indexedGetCall == null) throw AssertionError("Indexed LValue has no 'get' call: ${ktArrayAccessExpression.text}")
        if (indexedSetCall == null) throw AssertionError("Indexed LValue has no 'set' call: ${ktArrayAccessExpression.text}")

        val callGenerator = CallGenerator(statementGenerator)

        val irBlock = createDesugaredBlockWithTemporaries(indexedSetCall, callGenerator, true, irOperator)

        val irGetCall = callGenerator.generateCall(ktArrayAccessExpression, indexedGetCall, irOperator)
        val irTmp = callGenerator.scope.createTemporaryVariable(irGetCall)
        irBlock.addStatement(irTmp)

        val operatorCallReceiver = operatorCall.extensionReceiver ?: operatorCall.dispatchReceiver
        callGenerator.scope.putValue(operatorCallReceiver!!, VariableLValue(callGenerator, irTmp))
        val irOpCall = callGenerator.generateCall(ktArrayAccessExpression, operatorCall, irOperator)
        callGenerator.scope.putValue(indexedSetCall.resultingDescriptor.valueParameters.last(), SingleExpressionValue(irOpCall))
        val irSetCall = callGenerator.generateCall(ktArrayAccessExpression, indexedSetCall, irOperator)
        irBlock.addStatement(irSetCall)

        irBlock.addStatement(irTmp.defaultLoad())

        return irBlock
    }

    override fun augmentedStore(operatorCall: ResolvedCall<*>, irOperator: IrOperator, irOperatorArgument: IrExpression): IrExpression {
        if (indexedGetCall == null) throw AssertionError("Indexed LValue has no 'get' call: ${ktArrayAccessExpression.text}")
        if (indexedSetCall == null) throw AssertionError("Indexed LValue has no 'set' call: ${ktArrayAccessExpression.text}")

        val callGenerator = CallGenerator(statementGenerator)

        val irBlock = createDesugaredBlockWithTemporaries(indexedSetCall, callGenerator, false, irOperator)

        callGenerator.scope.putValue(operatorCall.resultingDescriptor.valueParameters[0], SingleExpressionValue(irOperatorArgument))

        val operatorCallReceiver = operatorCall.extensionReceiver ?: operatorCall.dispatchReceiver
        callGenerator.scope.putValue(operatorCallReceiver!!,
                                     SingleExpressionValue(callGenerator.generateCall(ktArrayAccessExpression, indexedGetCall, irOperator)))

        callGenerator.scope.putValue(indexedSetCall.resultingDescriptor.valueParameters.last(),
                                     SingleExpressionValue(callGenerator.generateCall(ktArrayAccessExpression, operatorCall, irOperator)))

        irBlock.addStatement(callGenerator.generateCall(ktArrayAccessExpression, indexedSetCall, irOperator))

        return irBlock
    }

    private fun createDesugaredBlockWithTemporaries(
            call: ResolvedCall<*>,
            callGenerator: CallGenerator,
            hasResult: Boolean,
            operator: IrOperator?
    ): IrBlockImpl {
        val irBlock = createDesugaredBlock(call, hasResult, operator)
        defineTemporaryVariables(irBlock, callGenerator.scope)
        return irBlock
    }

    private fun createDesugaredBlock(call: ResolvedCall<*>, hasResult: Boolean, operator: IrOperator?): IrBlockImpl {
        return IrBlockImpl(ktArrayAccessExpression.startOffset, ktArrayAccessExpression.endOffset,
                           if (hasResult) type else call.resultingDescriptor.returnType,
                           hasResult, operator)
    }

    private fun defineTemporaryVariables(irBlock: IrBlockImpl, scope: Scope) {
        irBlock.addIfNotNull(scope.introduceTemporary(ktArrayAccessExpression.arrayExpression!!, irArray, "array"))

        var index = 0
        for ((ktIndexExpression, irIndexValue) in indexValues) {
            irBlock.addIfNotNull(scope.introduceTemporary(ktIndexExpression, irIndexValue, "index${index++}"))
        }
    }

    private fun setupCallGeneratorContext(scope: Scope) {
        scope.putValue(ktArrayAccessExpression.arrayExpression!!, SingleExpressionValue(irArray))

        for ((ktIndexExpression, irIndexValue) in indexValues) {
            scope.putValue(ktIndexExpression, SingleExpressionValue(irIndexValue))
        }
    }
}