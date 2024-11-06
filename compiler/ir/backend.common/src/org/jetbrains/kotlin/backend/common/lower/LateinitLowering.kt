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

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.atMostOne

@PhaseDescription(
    name = "LateinitLowering",
)
open class LateinitLowering(
    private val loweringContext: LoweringContext,
    private val uninitializedPropertyAccessExceptionThrower: UninitializedPropertyAccessExceptionThrower,
) : FileLoweringPass, IrElementTransformerVoid() {
    private val visitedLateinitVariables = mutableSetOf<IrVariable>()

    constructor(loweringContext: LoweringContext) :
            this(loweringContext, UninitializedPropertyAccessExceptionThrower(loweringContext.ir.symbols))

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (declaration.isRealLateinit()) {
            val backingField = declaration.backingField!!
            if (!backingField.type.isMarkedNullable()) {
                transformLateinitBackingField(backingField, declaration)
                declaration.getter?.let {
                    transformGetter(backingField, it)
                }
            }
        }

        declaration.transformChildrenVoid()
        return declaration
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        declaration.transformChildrenVoid()

        if (declaration.isLateinit && !declaration.type.isMarkedNullable()) {
            visitedLateinitVariables += declaration
            declaration.type = declaration.type.makeNullable()
            declaration.isVar = true
            declaration.initializer =
                IrConstImpl.constNull(declaration.startOffset, declaration.endOffset, loweringContext.irBuiltIns.nothingNType)
        }

        return declaration
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        expression.transformChildrenVoid()

        val irValue = expression.symbol.owner
        if (irValue !is IrVariable || irValue !in visitedLateinitVariables) {
            return expression
        }

        return loweringContext.createIrBuilder(
            (irValue.parent as IrSymbolOwner).symbol,
            expression.startOffset,
            expression.endOffset
        ).run {
            irIfThenElse(
                expression.type,
                irEqualsNull(irGet(irValue)),
                uninitializedPropertyAccessExceptionThrower.build(this, irValue.name.asString()),
                irGet(irValue)
            )
        }
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        expression.transformChildrenVoid()

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
        expression.transformChildrenVoid()

        if (!Symbols.isLateinitIsInitializedPropertyGetter(expression.symbol)) return expression

        return expression.extensionReceiver!!.replaceTailExpression {
            val (property, dispatchReceiver) = when (it) {
                is IrPropertyReference -> it.getter?.owner?.resolveFakeOverride()?.correspondingPropertySymbol?.owner to it.dispatchReceiver
                is IrRichPropertyReference -> (it.reflectionTargetSymbol as? IrPropertySymbol)?.owner?.resolveFakeOverride() to it.boundValues.atMostOne()
                else -> error("Unsupported argument for KProperty::isInitialized call: ${it.render()}")
            }
            require(property?.isLateinit == true) {
                "isInitialized invoked on non-lateinit property ${property?.render()}"
            }
            val backingField = property.backingField
                ?: throw AssertionError("Lateinit property is supposed to have a backing field")
            transformLateinitBackingField(backingField, property)
            loweringContext.createIrBuilder(property.symbol, expression.startOffset, expression.endOffset).run {
                irNotEquals(
                    irGetField(dispatchReceiver, backingField),
                    irNull()
                )
            }
        }
    }

    protected open fun transformLateinitBackingField(backingField: IrField, property: IrProperty) {
        assert(backingField.initializer == null) {
            "lateinit property backing field should not have an initializer:\n${property.dump()}"
        }
        backingField.type = backingField.type.makeNullable()
    }

    private fun transformGetter(backingField: IrField, getter: IrFunction) {
        val type = backingField.type
        assert(!type.isPrimitiveType()) {
            "'lateinit' property type should not be primitive:\n${backingField.dump()}"
        }
        val startOffset = getter.startOffset
        val endOffset = getter.endOffset
        getter.body = loweringContext.irFactory.createBlockBody(startOffset, endOffset) {
            val irBuilder = loweringContext.createIrBuilder(getter.symbol, startOffset, endOffset)
            irBuilder.run {
                val resultVar = scope.createTmpVariable(
                    irGetField(getter.dispatchReceiverParameter?.let { irGet(it) }, backingField, type)
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
        uninitializedPropertyAccessExceptionThrower.build(this, name)
}

private inline fun IrExpression.replaceTailExpression(crossinline transform: (IrExpression) -> IrExpression): IrExpression {
    var current = this
    var block: IrContainerExpression? = null
    while (current is IrContainerExpression) {
        block = current
        current = current.statements.last() as IrExpression
    }
    current = transform(current)
    if (block == null) {
        return current
    }
    block.statements[block.statements.size - 1] = current
    return this
}

open class UninitializedPropertyAccessExceptionThrower(private val symbols: Symbols) {
    open fun build(builder: IrBuilderWithScope, name: String): IrExpression {
        val throwExceptionFunction = symbols.throwUninitializedPropertyAccessException.owner
        return builder.irCall(throwExceptionFunction).apply {
            putValueArgument(0, builder.irString(name))
        }
    }
}
