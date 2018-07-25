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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class LateinitLowering(
    val context: CommonBackendContext,
    private val generateParameterNameInAssertion: Boolean = false
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitProperty(declaration: IrProperty): IrStatement {
                if (declaration.isLateinit && declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE)
                    transformGetter(declaration.backingField!!, declaration.getter!!)
                return declaration
            }

            private fun transformGetter(backingField: IrField, getter: IrFunction) {
                val type = backingField.type
                assert(!type.isPrimitiveType()) { "'lateinit' modifier is not allowed on primitive types" }
                val startOffset = getter.startOffset
                val endOffset = getter.endOffset
                val irBuilder = context.createIrBuilder(getter.symbol, startOffset, endOffset)
                irBuilder.run {
                    val block = irBlock(type)
                    val resultVar = scope.createTemporaryVariable(
                        irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField)
                    )
                    block.statements.add(resultVar)
                    val throwIfNull = irIfThenElse(
                        context.irBuiltIns.nothingType,
                        irNotEquals(irGet(resultVar), irNull()),
                        irReturn(irGet(resultVar)),
                        throwUninitializedPropertyAccessException(backingField)
                    )
                    block.statements.add(throwIfNull)
                    getter.body = IrExpressionBodyImpl(startOffset, endOffset, block)
                }
            }
        })
    }

    private fun IrBuilderWithScope.throwUninitializedPropertyAccessException(backingField: IrField) =
        irCall(throwErrorFunction).apply {
            if (generateParameterNameInAssertion) {
                putValueArgument(
                    0,
                    IrConstImpl.string(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.stringType,
                        backingField.name.asString()
                    )
                )
            }
        }

    private val throwErrorFunction = context.ir.symbols.ThrowUninitializedPropertyAccessException.owner

    private fun IrBuilderWithScope.irBlock(type: IrType): IrBlock = IrBlockImpl(startOffset, endOffset, type)

}