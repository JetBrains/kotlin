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

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val kCallableNamePropertyPhase = makeIrFilePhase(
    ::KCallableNamePropertyLowering,
    name = "KCallableNameProperty",
    description = "Replace name references for callables with constants"
)

private class KCallableNamePropertyLowering(val context: BackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(KCallableNamePropertyTransformer(this))
    }
}

private class KCallableNamePropertyTransformer(val lower: KCallableNamePropertyLowering) : IrElementTransformerVoid() {

    override fun visitCall(expression: IrCall): IrExpression {

        val callableReference = expression.dispatchReceiver as? IrCallableReference ?: return expression

        //TODO rewrite checking
        val directMember = expression.symbol.owner.let {
            (it as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: it
        }
        val irClass = directMember.parent as? IrClass ?: return expression
        if (!irClass.isSubclassOf(lower.context.irBuiltIns.kCallableClass.owner)) return expression
        val name = when (directMember) {
            is IrSimpleFunction -> directMember.name
            is IrProperty -> directMember.name
            else -> throw AssertionError("Should be IrSimpleFunction or IrProperty, got $directMember")
        }
        if (name.asString() != "name") return expression

        val receiver = callableReference.dispatchReceiver ?: callableReference.extensionReceiver

        return IrCompositeImpl(expression.startOffset, expression.endOffset, lower.context.irBuiltIns.stringType).apply {
            receiver?.let {
                //put receiver for bound callable reference
                statements.add(it)
            }

            statements.add(
                IrConstImpl.string(
                    expression.startOffset,
                    expression.endOffset,
                    lower.context.irBuiltIns.stringType,
                    callableReference.symbol.descriptor.name.asString()
                )
            )
        }
    }
}
