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

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi2ir.generators.CallGenerator
import org.jetbrains.kotlin.types.KotlinType
import java.lang.AssertionError

class ArrayAccessAssignmentReceiver(
        val irArray: IrExpression,
        val irIndices: List<IrExpression>,
        val indexedGetCall: PregeneratedCall?,
        val indexedSetCall: PregeneratedCall?,
        val callGenerator: CallGenerator,
        val startOffset: Int,
        val endOffset: Int,
        val operator: IrOperator
) : AssignmentReceiver {
    private val type: KotlinType = indexedGetCall?.let { it.descriptor.returnType!! } ?:
                                   indexedSetCall?.let { it.descriptor.valueParameters.last().type } ?:
                                   throw AssertionError("Array access should have either indexed-get call or indexed-set call")

    override fun assign(withLValue: (IntermediateReference) -> IrExpression): IrExpression {
        val hasResult = operator.isAssignmentOperatorWithResult()
        val resultType = if (hasResult) type else callGenerator.context.builtIns.unitType
        val irBlock = IrBlockImpl(startOffset, endOffset, resultType, hasResult, operator)

        val irArrayValue = createRematerializableOrTemporary(callGenerator.scope, irArray, irBlock, "array")

        val irIndexValues = irIndices.mapIndexed { i, irIndex ->
            createRematerializableOrTemporary(callGenerator.scope, irIndex, irBlock, "index$i")
        }

        indexedGetCall?.fillArrayAndIndexArguments(irArrayValue, irIndexValues)
        indexedSetCall?.fillArrayAndIndexArguments(irArrayValue, irIndexValues)
        val irLValue = LValueWithGetterAndSetterCalls(callGenerator, indexedGetCall, indexedSetCall, startOffset, endOffset, operator)
        irBlock.inlineStatement(withLValue(irLValue))

        return irBlock
    }

    private fun PregeneratedCall.fillArrayAndIndexArguments(arrayValue: IntermediateValue, indexValues: List<IntermediateValue>) {
        setExplicitReceiverValue(arrayValue)
        indexValues.forEachIndexed { i, irIndexValue ->
            irValueArgumentsByIndex[i] = irIndexValue.load()
        }
    }
}
