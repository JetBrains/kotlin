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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType

class LateinitLowering(val context: CommonBackendContext): FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitProperty(declaration: IrProperty): IrStatement {
                if (declaration.descriptor.isLateInit && declaration.descriptor.kind.isReal)
                    transformGetter(declaration.backingField!!.symbol, declaration.getter!!)
                return declaration
            }

            private fun transformGetter(backingFieldSymbol: IrFieldSymbol, getter: IrFunction) {
                val type = backingFieldSymbol.descriptor.type
                assert (!KotlinBuiltIns.isPrimitiveType(type), { "'lateinit' modifier is not allowed on primitive types" })
                val startOffset = getter.startOffset
                val endOffset = getter.endOffset
                val irBuilder = context.createIrBuilder(getter.symbol, startOffset, endOffset)
                irBuilder.run {
                    val block = irBlock(type)
                    val resultVar = scope.createTemporaryVariable(
                            irGetField(getter.dispatchReceiverParameter?.let { irGet(it.symbol) }, backingFieldSymbol)
                    )
                    block.statements.add(resultVar)
                    val throwIfNull = irIfThenElse(context.builtIns.nothingType,
                            irNotEquals(irGet(resultVar.symbol), irNull()),
                            irReturn(irGet(resultVar.symbol)),
                            irCall(throwErrorFunction))
                    block.statements.add(throwIfNull)
                    getter.body = IrExpressionBodyImpl(startOffset, endOffset, block)
                }
            }
        })
    }

    private val throwErrorFunction = context.ir.symbols.ThrowUninitializedPropertyAccessException

    private fun IrBuilderWithScope.irBlock(type: KotlinType): IrBlock
            = IrBlockImpl(startOffset, endOffset, type)

}