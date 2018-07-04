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

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.psi2ir.generators.GeneratorWithScope
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.makeNullable


class SafeCallReceiver(
    val generator: GeneratorWithScope,
    val startOffset: Int,
    val endOffset: Int,
    val extensionReceiver: IntermediateValue?,
    val dispatchReceiver: IntermediateValue?,
    val isAssignmentReceiver: Boolean
) : CallReceiver {

    override fun call(withDispatchAndExtensionReceivers: (IntermediateValue?, IntermediateValue?) -> IrExpression): IrExpression {
        val irTmp = generator.scope.createTemporaryVariable(extensionReceiver?.load() ?: dispatchReceiver!!.load(), "safe_receiver")
        val safeReceiverValue = VariableLValue(generator.context, irTmp)

        val dispatchReceiverValue: IntermediateValue?
        val extensionReceiverValue: IntermediateValue?
        if (extensionReceiver != null) {
            dispatchReceiverValue = dispatchReceiver
            extensionReceiverValue = safeReceiverValue
        } else {
            dispatchReceiverValue = safeReceiverValue
            extensionReceiverValue = null
        }

        val irResult = withDispatchAndExtensionReceivers(dispatchReceiverValue, extensionReceiverValue)

        val resultType = if (isAssignmentReceiver) generator.context.irBuiltIns.unitType else irResult.type.makeNullable()

        val irBlock = IrBlockImpl(startOffset, endOffset, resultType, IrStatementOrigin.SAFE_CALL)

        irBlock.statements.add(irTmp)

        val irIfThenElse =
            generator.buildStatement(startOffset, endOffset, IrStatementOrigin.SAFE_CALL) {
                irIfNull(resultType, safeReceiverValue.load(), irNull(), irResult)
            }
        irBlock.statements.add(irIfThenElse)

        return irBlock
    }
}


fun IrExpression.safeCallOnDispatchReceiver(
    generator: GeneratorWithScope,
    startOffset: Int,
    endOffset: Int,
    ifNotNull: (IrExpression) -> IrExpression
) =
    SafeCallReceiver(
        generator, startOffset, endOffset,
        extensionReceiver = null,
        dispatchReceiver = OnceExpressionValue(this),
        isAssignmentReceiver = false
    ).call { dispatchReceiverValue, _ ->
        ifNotNull(dispatchReceiverValue!!.load())
    }