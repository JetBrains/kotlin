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
import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class LateinitLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val nullableFields = mutableMapOf<IrField, IrField>()
        val nullableVariables = mutableMapOf<IrVariable, IrVariable>()

        // Transform declarations
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitProperty(declaration: IrProperty): IrStatement {
                declaration.transformChildrenVoid(this)
                if (declaration.isLateinit && declaration.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                    val oldField = declaration.backingField!!
                    val newField = buildField {
                        updateFrom(oldField)
                        type = oldField.type.makeNullable()
                        name = oldField.name
                    }.also { newField ->
                        newField.parent = oldField.parent
                        declaration.backingField = newField
                    }

                    nullableFields[oldField] = newField

                    transformGetter(newField, declaration.getter!!)
                }
                return declaration
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                declaration.transformChildrenVoid(this)

                if (!declaration.isLateinit) return declaration

                val descriptor = WrappedVariableDescriptor()
                val type = declaration.type.makeNullable()
                val newVar = IrVariableImpl(
                    declaration.startOffset,
                    declaration.endOffset,
                    declaration.origin,
                    IrVariableSymbolImpl(descriptor),
                    declaration.name,
                    type,
                    true,
                    false,
                    true
                ).also {
                    descriptor.bind(it)
                    it.parent = declaration.parent
                    it.initializer = IrConstImpl.constNull(declaration.startOffset, declaration.endOffset, type)
                }

                nullableVariables[declaration] = newVar

                return newVar
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

        // Transform usages
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val irVar = nullableVariables[expression.symbol.owner] ?: return expression

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

            override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                expression.transformChildrenVoid(this)
                val newVar = nullableVariables[expression.symbol.owner] ?: return expression
                return with(expression) {
                    IrSetVariableImpl(startOffset, endOffset, type, newVar.symbol, value, origin)
                }
            }

            override fun visitGetField(expression: IrGetField): IrExpression {
                expression.transformChildrenVoid(this)
                val newField = nullableFields[expression.symbol.owner] ?: return expression
                return with(expression) {
                    IrGetFieldImpl(startOffset, endOffset, newField.symbol, newField.type, receiver, origin, superQualifierSymbol)
                }
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                expression.transformChildrenVoid(this)
                val newField = nullableFields[expression.symbol.owner] ?: return expression
                return with(expression) {
                    IrSetFieldImpl(startOffset, endOffset, newField.symbol, receiver, value, type, origin, superQualifierSymbol)
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
