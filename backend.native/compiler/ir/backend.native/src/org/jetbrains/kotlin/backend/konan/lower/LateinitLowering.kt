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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class LateinitLowering(
        val context: CommonBackendContext,
        private val generateParameterNameInAssertion: Boolean = false
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val symbol = expression.symbol
                val descriptor = symbol.descriptor as? VariableDescriptor
                if (descriptor == null || !descriptor.isLateInit) return expression

                assert(!KotlinBuiltIns.isPrimitiveType(descriptor.type), { "'lateinit' modifier is not allowed on primitive types" })
                val startOffset = expression.startOffset
                val endOffset = expression.endOffset
                val irBuilder = context.createIrBuilder(symbol, startOffset, endOffset)
                irBuilder.run {
                    return irBlock(expression) {
                        // TODO: do data flow analysis to check if value is proved to be not-null.
                        +irIfThen(
                                irEqualsNull(irGet(symbol)),
                                throwUninitializedPropertyAccessException(symbol)
                        )
                        +irGet(symbol)
                    }
                }
            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                declaration.transformChildrenVoid(this)

                if (declaration.descriptor.isLateInit && declaration.descriptor.kind.isReal)
                    transformGetter(declaration.backingField!!.symbol, declaration.getter!!)

                return declaration
            }

            private fun transformGetter(backingFieldSymbol: IrFieldSymbol, getter: IrFunction) {
                val type = backingFieldSymbol.descriptor.type
                assert(!KotlinBuiltIns.isPrimitiveType(type), { "'lateinit' modifier is not allowed on primitive types" })
                val startOffset = getter.startOffset
                val endOffset = getter.endOffset
                val irBuilder = context.createIrBuilder(getter.symbol, startOffset, endOffset)
                irBuilder.run {
                    getter.body = irBlockBody {
                        val resultVar = irTemporary(
                                irGetField(getter.dispatchReceiverParameter?.let { irGet(it.symbol) }, backingFieldSymbol)
                        )
                        +irIfThenElse(
                                context.builtIns.nothingType,
                                irNotEquals(irGet(resultVar.symbol), irNull()),
                                irReturn(irGet(resultVar.symbol)),
                                throwUninitializedPropertyAccessException(backingFieldSymbol)
                        )
                    }
                }
            }
        })
    }

    private fun IrBuilderWithScope.throwUninitializedPropertyAccessException(backingFieldSymbol: IrSymbol) =
            irCall(throwErrorFunction).apply {
                if (generateParameterNameInAssertion) {
                    putValueArgument(
                            0,
                            IrConstImpl.string(
                                    startOffset,
                                    endOffset,
                                    context.builtIns.stringType,
                                    backingFieldSymbol.descriptor.name.asString()
                            )
                    )
                }
            }

    private val throwErrorFunction = context.ir.symbols.ThrowUninitializedPropertyAccessException

}