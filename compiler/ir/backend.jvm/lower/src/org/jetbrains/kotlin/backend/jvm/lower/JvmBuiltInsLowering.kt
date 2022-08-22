/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.render

internal val jvmBuiltInsPhase = makeIrFilePhase(
    ::JvmBuiltInsLowering,
    name = "JvmBuiltInsLowering",
    description = "JVM-specific implementations of some built-ins"
)

class JvmBuiltInsLowering(val context: JvmBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        val transformer = object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildren(this, null)

                val callee = expression.symbol.owner

                if (context.state.target >= JvmTarget.JVM_1_8) {
                    val parentClassName = callee.parent.fqNameForIrSerialization.asString()
                    val functionName = callee.name.asString()
                    val jvm8Replacement = jvm8builtInReplacements[parentClassName to functionName]
                    if (jvm8Replacement != null) {
                        return expression.replaceWithCallTo(jvm8Replacement)
                    }
                }

                return when {
                    callee.isArrayOf() ->
                        expression.getValueArgument(0)
                            ?: throw AssertionError("Argument #0 expected: ${expression.dump()}")

                    callee.isEmptyArray() ->
                        context.createJvmIrBuilder(currentScope!!, expression).irArrayOf(expression.type)

                    else ->
                        expression
                }
            }
        }

        irFile.transformChildren(transformer, null)
    }

    private val jvm8builtInReplacements = mapOf(
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
    // If we can't coerce arguments to required types, keep original expression (see below).
    private fun IrCall.replaceWithCallTo(replacement: IrSimpleFunctionSymbol): IrExpression {
        val expectedType = this.type
        val intrinsicCallType = replacement.owner.returnType

        val intrinsicCall = IrCallImpl.fromSymbolOwner(
            startOffset,
            endOffset,
            intrinsicCallType,
            replacement
        ).also { newCall ->
            var valueArgumentOffset = 0
            this.dispatchReceiver?.let {
                val coercedDispatchReceiver = it.coerceIfPossible(replacement.owner.valueParameters[valueArgumentOffset].type)
                    ?: return this@replaceWithCallTo
                newCall.putValueArgument(valueArgumentOffset, coercedDispatchReceiver)
                valueArgumentOffset++
            }
            for (index in 0 until valueArgumentsCount) {
                val coercedValueArgument = getValueArgument(index)!!.coerceIfPossible(replacement.owner.valueParameters[index].type)
                    ?: return this@replaceWithCallTo
                newCall.putValueArgument(index + valueArgumentOffset, coercedValueArgument)
            }
        }

        // Coerce intrinsic call result from JVM 'int' or 'long' to corresponding unsigned type if required.
        return if (intrinsicCallType.isInt() || intrinsicCallType.isLong()) {
            intrinsicCall.coerceIfPossible(expectedType)
                ?: throw AssertionError("Can't coerce '${intrinsicCallType.render()}' to '${expectedType.render()}'")
        } else {
            intrinsicCall
        }
    }

    private fun IrExpression.coerceIfPossible(toType: IrType): IrExpression? {
        // TODO maybe UnsafeCoerce could handle types with different, but coercible underlying representations.
        // See KT-43286 and related tests for details.
        val fromJvmType = context.defaultTypeMapper.mapType(type)
        val toJvmType = context.defaultTypeMapper.mapType(toType)
        return if (fromJvmType != toJvmType)
            null
        else
            IrCallImpl.fromSymbolOwner(startOffset, endOffset, toType, context.ir.symbols.unsafeCoerceIntrinsic).also { call ->
                call.putTypeArgument(0, type)
                call.putTypeArgument(1, toType)
                call.putValueArgument(0, this)
            }
    }
}
