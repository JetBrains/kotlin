/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.ir.builders.constNull
import org.jetbrains.kotlin.ir.builders.equalsNull
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.psi2ir.generators.GeneratorWithScope
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class SafeExtensionInvokeCallReceiver(
    val generator: GeneratorWithScope,
    val startOffset: Int,
    val endOffset: Int,
    val callBuilder: CallBuilder,
    val functionReceiver: IntermediateValue,
    val extensionInvokeReceiver: IntermediateValue
) : CallReceiver {
    override fun call(withDispatchAndExtensionReceivers: (IntermediateValue?, IntermediateValue?) -> IrExpression): IrExpression {
        // extensionInvokeReceiver is actually a first argument:
        //      receiver?.extFun(p1, ..., pN)
        //      =>
        //      val tmp = [receiver]
        //      if (tmp == null) null else extFun.invoke(tmp, p1, ..., pN)

        val irTmp = generator.scope.createTemporaryVariable(extensionInvokeReceiver.load(), "safe_receiver")

        val safeReceiverValue = VariableLValue(irTmp)

        // Patch call and generate it
        assert(callBuilder.irValueArgumentsByIndex[0] == null) {
            "Safe extension 'invoke' call should have null as its 1st value argument, got: ${callBuilder.irValueArgumentsByIndex[0]}"
        }
        callBuilder.irValueArgumentsByIndex[0] = safeReceiverValue.load()
        val irResult = withDispatchAndExtensionReceivers(functionReceiver, null)

        val resultType = irResult.type.makeNullable()

        return IrBlockImpl(
            startOffset, endOffset, resultType, IrStatementOrigin.SAFE_CALL,
            arrayListOf(
                irTmp,
                IrIfThenElseImpl(
                    startOffset, endOffset, resultType,
                    generator.context.equalsNull(startOffset, endOffset, safeReceiverValue.load()),
                    generator.context.constNull(startOffset, endOffset),
                    irResult,
                    IrStatementOrigin.SAFE_CALL
                )
            )
        )
    }
}

