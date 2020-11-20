/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val jvmStandardLibraryBuiltInsPhase = makeIrFilePhase(
    ::JvmStandardLibraryBuiltInsLowering,
    name = "JvmStandardLibraryBuiltInsLowering",
    description = "Use Java Standard Library implementations of built-ins"
)

class JvmStandardLibraryBuiltInsLowering(val context: JvmBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        if (context.state.target < JvmTarget.JVM_1_8) return

        val transformer = object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildren(this, null)

                val parentClass = expression.symbol.owner.parent.fqNameForIrSerialization.asString()
                val functionName = expression.symbol.owner.name.asString()
                Jvm8builtInReplacements[parentClass to functionName]?.let {
                    return expression.replaceWithCallTo(it)
                }

                return expression
            }
        }

        irFile.transformChildren(transformer, null)
    }

    private val Jvm8builtInReplacements = mapOf(
        ("kotlin.UInt" to "compareTo") to context.ir.symbols.compareUnsignedInt,
        ("kotlin.UInt" to "div") to context.ir.symbols.divideUnsignedInt,
        ("kotlin.UInt" to "rem") to context.ir.symbols.remainderUnsignedInt,
        ("kotlin.UInt" to "toString") to context.ir.symbols.toUnsignedStringInt,
        ("kotlin.ULong" to "compareTo") to context.ir.symbols.compareUnsignedLong,
        ("kotlin.ULong" to "div") to context.ir.symbols.divideUnsignedLong,
        ("kotlin.ULong" to "rem") to context.ir.symbols.remainderUnsignedLong,
        ("kotlin.ULong" to "toString") to context.ir.symbols.toUnsignedStringLong
    )

    // Originals are so far only instance methods, and the replacements are
    // statics, so we copy dispatch receivers to a value argument if needed.
    private fun IrCall.replaceWithCallTo(replacement: IrSimpleFunctionSymbol) =
        IrCallImpl.fromSymbolOwner(
            startOffset,
            endOffset,
            type,
            replacement
        ).also { newCall ->
            var valueArgumentOffset = 0
            this.dispatchReceiver?.let {
                newCall.putValueArgument(valueArgumentOffset, it.coerceTo(replacement.owner.valueParameters[valueArgumentOffset].type))
                valueArgumentOffset++
            }
            (0 until valueArgumentsCount).forEach {
                newCall.putValueArgument(it + valueArgumentOffset, getValueArgument(it)!!.coerceTo(replacement.owner.valueParameters[it].type))
            }
        }

    private fun IrExpression.coerceTo(target: IrType): IrExpression =
        IrCallImpl.fromSymbolOwner(
            startOffset,
            endOffset,
            target,
            context.ir.symbols.unsafeCoerceIntrinsic
        ).also { call ->
            call.putTypeArgument(0, type)
            call.putTypeArgument(1, target)
            call.putValueArgument(0, this)
        }
}
