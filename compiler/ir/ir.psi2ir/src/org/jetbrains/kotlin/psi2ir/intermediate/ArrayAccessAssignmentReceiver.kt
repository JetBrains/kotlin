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

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.inlineStatement
import org.jetbrains.kotlin.ir.expressions.isAssignmentOperatorWithResult
import org.jetbrains.kotlin.psi2ir.generators.CallGenerator
import org.jetbrains.kotlin.types.KotlinType
import java.lang.AssertionError

class ArrayAccessAssignmentReceiver(
    private val irArray: IrExpression,
    private val irIndices: List<IrExpression>,
    private val indexedGetCall: CallBuilder?,
    private val indexedSetCall: CallBuilder?,
    private val callGenerator: CallGenerator,
    private val startOffset: Int,
    private val endOffset: Int,
    private val origin: IrStatementOrigin
) : AssignmentReceiver {

    private val kotlinType: KotlinType =
        indexedGetCall?.run { descriptor.returnType!! } ?: indexedSetCall?.run { descriptor.valueParameters.last().type }
        ?: throw AssertionError("Array access should have either indexed-get call or indexed-set call")

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression {
        val hasResult = origin.isAssignmentOperatorWithResult()
        val resultType = if (hasResult) kotlinType else callGenerator.context.builtIns.unitType
        val irResultType = callGenerator.translateType(resultType)
        val irBlock = IrBlockImpl(startOffset, endOffset, irResultType, origin)

        val irArrayValue = callGenerator.scope.createTemporaryVariableInBlock(callGenerator.context, irArray, irBlock, "array")

        val irIndexValues = irIndices.mapIndexed { i, irIndex ->
            callGenerator.scope.createTemporaryVariableInBlock(callGenerator.context, irIndex, irBlock, "index$i")
        }

        indexedGetCall?.fillArrayAndIndexArguments(irArrayValue, irIndexValues)
        indexedSetCall?.fillArrayAndIndexArguments(irArrayValue, irIndexValues)
        val irLValue = LValueWithGetterAndSetterCalls(
            callGenerator,
            indexedGetCall, indexedSetCall,
            callGenerator.translateType(kotlinType),
            startOffset, endOffset, origin
        )
        irBlock.inlineStatement(withLValue(irLValue))

        return irBlock
    }

    override fun assign(value: IrExpression): IrExpression {
        if (indexedSetCall == null) throw AssertionError("Array access without indexed-get call")
        indexedSetCall.setExplicitReceiverValue(OnceExpressionValue(irArray))
        irIndices.forEachIndexed { i, irIndex ->
            indexedSetCall.irValueArgumentsByIndex[i] = irIndex
        }
        indexedSetCall.lastArgument = value
        return callGenerator.generateCall(startOffset, endOffset, indexedSetCall, IrStatementOrigin.EQ)
    }

    private fun CallBuilder.fillArrayAndIndexArguments(arrayValue: IntermediateValue, indexValues: List<IntermediateValue>) {
        setExplicitReceiverValue(arrayValue)
        indexValues.forEachIndexed { i, irIndexValue ->
            irValueArgumentsByIndex[i] = irIndexValue.load()
        }
    }
}
