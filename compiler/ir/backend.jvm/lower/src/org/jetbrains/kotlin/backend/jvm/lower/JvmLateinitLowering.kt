/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.replaceTailExpression
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.types.isMarkedNullable
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
        irFile.transformChildrenVoid(Transformer(context))
    }

    private class Transformer(private val backendContext: JvmBackendContext) : IrElementTransformerVoid() {
        private val backingVariables = HashMap<IrVariable, IrVariable>()

        override fun visitField(declaration: IrField): IrStatement {
            if (declaration.isLateinitBackingField()) {
                assert(declaration.initializer == null) {
                    "lateinit property backing field should not have an initializer:\n${declaration.dump()}"
                }
                return getOrBuildLateinitBackingField(declaration)
            }

            declaration.transformChildrenVoid()
            return declaration
        }

        override fun visitVariable(declaration: IrVariable): IrStatement {
            declaration.transformChildrenVoid(this)
            if (!declaration.isLateinit) return declaration
            return getOrBuildLateinitBackingVar(declaration)
        }

        private fun getOrBuildLateinitBackingVar(declaration: IrVariable): IrVariable =
            backingVariables.getOrPut(declaration) {
                buildVariable(
                    declaration.parent,
                    declaration.startOffset,
                    declaration.endOffset,
                    declaration.origin,
                    declaration.name,
                    declaration.type.makeNullable(),
                    isVar = true,
                ).also {
                    it.initializer =
                        IrConstImpl.constNull(declaration.startOffset, declaration.endOffset, backendContext.irBuiltIns.nothingNType)
                }
            }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
            val property = declaration.correspondingPropertySymbol?.owner
            if (property != null && property.isRealLateinit() && declaration == property.getter) {
                transformGetter(getOrBuildLateinitBackingField(property.backingField!!), declaration)
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
            val irBackingVar = backingVariables[irValue]
                ?: throw AssertionError("Lateinit variable reference before use: ${expression.dump()}")

            return backendContext.createIrBuilder(
                (irBackingVar.parent as IrSymbolOwner).symbol,
                expression.startOffset,
                expression.endOffset
            ).run {
                irIfThenElse(
                    expression.type,
                    irEqualsNull(irGet(irBackingVar)),
                    backendContext.throwUninitializedPropertyAccessException(this, irBackingVar.name.asString()),
                    irGet(irBackingVar)
                )
            }
        }

        override fun visitSetValue(expression: IrSetValue): IrExpression {
            expression.transformChildrenVoid(this)
            val irValue = expression.symbol.owner
            if (irValue !is IrVariable || !irValue.isLateinit) {
                return expression
            }

            val irBackingVar = backingVariables[expression.symbol.owner]
                ?: throw AssertionError("Lateinit variable reference before use: ${expression.dump()}")
            return with(expression) {
                IrSetValueImpl(startOffset, endOffset, type, irBackingVar.symbol, value, origin)
            }
        }

        override fun visitGetField(expression: IrGetField): IrExpression {
            expression.transformChildrenVoid(this)
            val irField = expression.symbol.owner
            if (!irField.isLateinitBackingField()) {
                return expression
            }
            val newField = getOrBuildLateinitBackingField(irField)
            return with(expression) {
                IrGetFieldImpl(startOffset, endOffset, newField.symbol, newField.type, receiver, origin, superQualifierSymbol)
            }
        }

        override fun visitSetField(expression: IrSetField): IrExpression {
            expression.transformChildrenVoid(this)
            val irField = expression.symbol.owner
            if (!irField.isLateinitBackingField()) {
                return expression
            }
            val newField = getOrBuildLateinitBackingField(irField)
            return with(expression) {
                IrSetFieldImpl(startOffset, endOffset, newField.symbol, receiver, value, type, origin, superQualifierSymbol)
            }
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
                val newField = getOrBuildLateinitBackingField(backingField)
                backendContext.createIrBuilder(it.symbol, expression.startOffset, expression.endOffset).run {
                    irNotEquals(
                        irGetField(it.dispatchReceiver, newField),
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
                        irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField)
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

        private fun getOrBuildLateinitBackingField(originalField: IrField): IrField =
            if (originalField.type.isMarkedNullable())
                originalField
            else
                backendContext.mapping.lateInitFieldToNullableField.getOrPut(originalField) {
                    backendContext.irFactory.buildField {
                        updateFrom(originalField)
                        type = originalField.type.makeNullable()
                        name = originalField.name
                    }.apply {
                        parent = originalField.parent
                        correspondingPropertySymbol = originalField.correspondingPropertySymbol
                        annotations = originalField.annotations
                    }
                }

    }
}