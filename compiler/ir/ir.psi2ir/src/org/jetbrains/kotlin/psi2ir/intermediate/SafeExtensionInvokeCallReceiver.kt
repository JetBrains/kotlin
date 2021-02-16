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

import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.psi2ir.generators.GeneratorWithScope

class SafeExtensionInvokeCallReceiver(
    val generator: GeneratorWithScope,
    val startOffset: Int,
    val endOffset: Int,
    val callBuilder: CallBuilder,
    val functionReceiver: IntermediateValue,
    val extensionInvokeReceiver: IntermediateValue
) : CallReceiver {

    override fun call(withDispatchAndExtensionAndContextReceivers: (IntermediateValue?, IntermediateValue?, List<IntermediateValue>) -> IrExpression): IrExpression {
        // extensionInvokeReceiver is actually a first argument:
        //      receiver?.extFun(p1, ..., pN)
        //      =>
        //      val tmp = [receiver]
        //      if (tmp == null) null else extFun.invoke(tmp, p1, ..., pN)

        val irTmp = generator.scope.createTemporaryVariable(extensionInvokeReceiver.load(), "safe_receiver")

        val safeReceiverValue = VariableLValue(generator.context, irTmp)

        // Patch call and generate it
        assert(callBuilder.irValueArgumentsByIndex[0] == null) {
            "Safe extension 'invoke' call should have null as its 1st value argument, got: ${callBuilder.irValueArgumentsByIndex[0]}"
        }
        callBuilder.irValueArgumentsByIndex[0] = safeReceiverValue.load()
        val irResult = withDispatchAndExtensionAndContextReceivers(functionReceiver, null, emptyList())

        val resultType = irResult.type.makeNullable()

        return generator.irBlock(startOffset, endOffset, IrStatementOrigin.SAFE_CALL, resultType) {
            +irTmp
            +irIfNull(
                resultType,
                safeReceiverValue.load(), irNull(),
                irResult
            )
        }
    }
}

