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

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val jvmLateinitPhase = makeIrFilePhase(
    ::LateinitLowering,
    name = "Lateinit",
    description = "Insert checks for lateinit field references"
)

class LateinitLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitProperty(declaration: IrProperty): IrStatement {
                declaration.transformChildrenVoid(this)
                if (declaration.isLateinit && declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                    transformGetter(declaration.backingField!!, declaration.getter!!)
                }
                return declaration
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                declaration.transformChildrenVoid(this)

                if (!declaration.isLateinit) return declaration

                declaration.run { initializer = IrConstImpl.constNull(startOffset, endOffset, type) }

                return declaration
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val irVar = expression.symbol.owner as? IrVariable ?: return expression

                if (!irVar.isLateinit) return expression

                val parent = irVar.parent as IrSymbolOwner

                val irBuilder = context.createIrBuilder(parent.symbol, expression.startOffset, expression.endOffset)

                return irBuilder.run {
                    irIfThenElse(
                        expression.type, irEqualsNull(irGet(irVar)),
                        throwUninitializedPropertyAccessException(irVar.name.asString()),
                        irGet(irVar)
                    )
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (!Symbols.isLateinitIsInitializedPropertyGetter(expression.symbol)) return expression

                val receiver = expression.extensionReceiver as IrPropertyReference

                val property = receiver.getter?.owner?.resolveFakeOverride()?.correspondingProperty!!.also { assert(it.isLateinit) }

                return expression.run { context.createIrBuilder(symbol, startOffset, endOffset) }.run {
                    irNotEquals(irGetField(receiver.dispatchReceiver, property.backingField!!), irNull())
                }
            }

            private fun transformGetter(backingField: IrField, getter: IrFunction) {
                val type = backingField.type
                assert(!type.isPrimitiveType()) { "'lateinit' modifier is not allowed on primitive types" }
                val startOffset = getter.startOffset
                val endOffset = getter.endOffset
                val irBuilder = context.createIrBuilder(getter.symbol, startOffset, endOffset)
                irBuilder.run {
                    val body = IrBlockBodyImpl(startOffset, endOffset)
                    val resultVar = scope.createTemporaryVariable(
                        irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField)
                    )
                    resultVar.parent = getter
                    body.statements.add(resultVar)
                    val throwIfNull = irIfThenElse(
                        context.irBuiltIns.nothingType,
                        irNotEquals(irGet(resultVar), irNull()),
                        irReturn(irGet(resultVar)),
                        throwUninitializedPropertyAccessException(backingField.name.asString())
                    )
                    body.statements.add(throwIfNull)
                    getter.body = body
                }
            }
        })
    }

    private fun IrBuilderWithScope.throwUninitializedPropertyAccessException(name: String) =
        irCall(throwErrorFunction).apply {
            putValueArgument(
                0,
                IrConstImpl.string(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    name
                )
            )
        }

    private val throwErrorFunction by lazy { context.ir.symbols.ThrowUninitializedPropertyAccessException.owner }
}
