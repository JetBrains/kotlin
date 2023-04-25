/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.replaceTailExpression
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val jvmLateinitLowering = makeIrFilePhase(
    ::JvmLateinitLowering,
    name = "JvmLateinitLowering",
    description = "Lower lateinit properties and variables"
)


class JvmLateinitLowering(
    private val context: JvmBackendContext
) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        val transformer = Transformer(context)
        irFile.transformChildrenVoid(transformer)

        for (variable in transformer.lateinitVariables) {
            variable.isLateinit = false
        }
    }

    private class Transformer(private val backendContext: JvmBackendContext) : IrElementTransformerVoid() {
        val lateinitVariables = mutableListOf<IrVariable>()

        override fun visitField(declaration: IrField): IrStatement {
            if (declaration.isLateinitBackingField()) {
                assert(declaration.initializer == null) {
                    "lateinit property backing field should not have an initializer:\n${declaration.dump()}"
                }

                declaration.type = declaration.type.makeNullable()
            }

            declaration.transformChildrenVoid()
            return declaration
        }

        override fun visitVariable(declaration: IrVariable): IrStatement {
            declaration.transformChildrenVoid(this)

            if (declaration.isLateinit) {
                declaration.type = declaration.type.makeNullable()
                declaration.isVar = true
                declaration.initializer =
                    IrConstImpl.constNull(declaration.startOffset, declaration.endOffset, backendContext.irBuiltIns.nothingNType)

                lateinitVariables += declaration
            }

            return declaration
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
            val property = declaration.correspondingPropertySymbol?.owner
            if (property != null && property.isRealLateinit() && declaration == property.getter) {
                transformGetter(property.backingField!!, declaration)
                return declaration
            }

            declaration.transformChildrenVoid()
            return declaration
        }

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val irValue = expression.symbol.owner
            if (irValue !is IrVariable || !irValue.isLateinit) {
                return expression
            }

            return backendContext.createIrBuilder(
                (irValue.parent as IrSymbolOwner).symbol,
                expression.startOffset,
                expression.endOffset
            ).run {
                irIfThenElse(
                    expression.type,
                    irEqualsNull(irGet(irValue)),
                    backendContext.throwUninitializedPropertyAccessException(this, irValue.name.asString()),
                    irGet(irValue)
                )
            }
        }

        override fun visitGetField(expression: IrGetField): IrExpression {
            expression.transformChildrenVoid(this)
            val irField = expression.symbol.owner
            if (irField.isLateinitBackingField()) {
                expression.type = expression.type.makeNullable()
            }
            return expression
        }

        private fun IrField.isLateinitBackingField(): Boolean {
            val property = this.correspondingPropertySymbol?.owner
            return property != null && property.isRealLateinit()
        }

        private fun IrProperty.isRealLateinit() =
            isLateinit && !isFakeOverride

        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid(this)

            if (!Symbols.isLateinitIsInitializedPropertyGetter(expression.symbol)) return expression

            return expression.extensionReceiver!!.replaceTailExpression {
                val irPropertyRef = it as? IrPropertyReference
                    ?: throw AssertionError("Property reference expected: ${it.render()}")
                val property = irPropertyRef.getter?.owner?.resolveFakeOverride()?.correspondingPropertySymbol?.owner
                    ?: throw AssertionError("isInitialized cannot be invoked on ${it.render()}")
                require(property.isLateinit) {
                    "isInitialized invoked on non-lateinit property ${property.render()}"
                }
                val backingField = property.backingField
                    ?: throw AssertionError("Lateinit property is supposed to have a backing field")
                backendContext.createIrBuilder(it.symbol, expression.startOffset, expression.endOffset).run {
                    irNotEquals(
                        irGetField(it.dispatchReceiver, backingField),
                        irNull()
                    )
                }
            }
        }

        private fun transformGetter(backingField: IrField, getter: IrFunction) {
            val type = backingField.type
            assert(!type.isPrimitiveType()) {
                "'lateinit' property type should not be primitive:\n${backingField.dump()}"
            }
            val startOffset = getter.startOffset
            val endOffset = getter.endOffset
            getter.body = backendContext.irFactory.createBlockBody(startOffset, endOffset) {
                val irBuilder = backendContext.createIrBuilder(getter.symbol, startOffset, endOffset)
                irBuilder.run {
                    val resultVar = scope.createTmpVariable(
                        irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField, backingField.type.makeNullable())
                    )
                    resultVar.parent = getter
                    statements.add(resultVar)
                    val throwIfNull = irIfThenElse(
                        context.irBuiltIns.nothingType,
                        irNotEquals(irGet(resultVar), irNull()),
                        irReturn(irGet(resultVar)),
                        throwUninitializedPropertyAccessException(backingField.name.asString())
                    )
                    statements.add(throwIfNull)
                }
            }
        }

        private fun IrBuilderWithScope.throwUninitializedPropertyAccessException(name: String) =
            backendContext.throwUninitializedPropertyAccessException(this, name)
    }
}
