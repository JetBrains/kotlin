/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.atMostOne

@PhaseDescription(
    name = "LateinitIsInitializedLowering",
)
open class LateinitIsInitializedLowering(
    private val loweringContext: LoweringContext,
) : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()

        if (!Symbols.Companion.isLateinitIsInitializedPropertyGetter(expression.symbol)) return expression

        return expression.arguments[0]!!.replaceTailExpression {
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
            loweringContext.createIrBuilder(property.symbol, expression.startOffset, expression.endOffset).run {
                irNotEquals(
                    irGetField(dispatchReceiver, backingField),
                    irNull()
                )
            }
        }
    }
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