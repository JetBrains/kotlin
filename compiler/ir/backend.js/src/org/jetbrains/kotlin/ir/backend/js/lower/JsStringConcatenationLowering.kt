/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

class JsStringConcatenationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(JsStringConcatenationTransformer(context))
    }
}

private class JsStringConcatenationTransformer(val context: CommonBackendContext) : IrElementTransformerVoid() {
    private val IrType.shouldExplicitlyConvertToString: Boolean
        get() {
            if (this !is IrSimpleType) return false
            /**
             * The type may have a valueOf() function, meaning that in string concatenation,
             * the toString() function will be ignored, and the valueOf() function will be called instead.
             * Therefore, we have to wrap all types except those where we are sure that they don't have the valueOf() function.
             *
             * Note, that we do not check for the existence of the valueOf() function
             * in the class because it would complicate incremental compilation.
             *
             * Ignore [Long] and all its supertypes ([Any], [Comparable], [Number]) since [Long] has the valueOf() method.
             * Ignore [Char] since it requires an explicit conversion to string.
             */
            return when (classifier.signature) {
                IdSignatureValues._boolean, IdSignatureValues.string, IdSignatureValues.array,
                IdSignatureValues._byte, IdSignatureValues._short, IdSignatureValues._int,
                IdSignatureValues.uByte, IdSignatureValues.uShort, IdSignatureValues.uInt, IdSignatureValues.uLong,
                IdSignatureValues._float, IdSignatureValues._double,
                -> false
                else -> true
            }
        }

    private fun IrExpression.explicitlyConvertedToString(): IrExpression {
        assert(type.shouldExplicitlyConvertToString)

        return if (type.isNullable()) {
            JsIrBuilder.buildCall(context.ir.symbols.extensionToString).apply {
                extensionReceiver = this@explicitlyConvertedToString
            }
        } else {
            val anyToStringMethodSymbol = context.ir.symbols.memberToString
            val toStringMethodSymbol = type.classOrNull?.let {
                val toStringMethods = it.owner.declarations.filterIsInstanceAnd<IrSimpleFunction> { f ->
                    f.overrides(anyToStringMethodSymbol.owner)
                }
                toStringMethods.singleOrNull()?.symbol
            } ?: anyToStringMethodSymbol

            JsIrBuilder.buildCall(toStringMethodSymbol).apply {
                dispatchReceiver = this@explicitlyConvertedToString
            }
        }
    }

    private val IrFunctionSymbol.isStringPlus: Boolean
        get() = context.ir.symbols.isStringPlus(this)

    override fun visitCall(expression: IrCall): IrExpression {
        fun explicitlyConvertToStringIfNeeded(): IrExpression {
            val lastArgIndex = expression.valueArgumentsCount - 1
            val plusArg = expression.getValueArgument(lastArgIndex) ?: return super.visitCall(expression)
            if (!plusArg.type.shouldExplicitlyConvertToString)
                return super.visitCall(expression)

            expression.putValueArgument(lastArgIndex, plusArg.explicitlyConvertedToString())
            return expression
        }

        if (expression.valueArgumentsCount == 0)
            return super.visitCall(expression)

        if (expression.symbol.isStringPlus)
            return explicitlyConvertToStringIfNeeded()

        if (expression.dispatchReceiver.safeAs<IrFunctionReference>()?.symbol?.isStringPlus == true)
            return explicitlyConvertToStringIfNeeded()

        return super.visitCall(expression)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression
            .transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitExpression(expression: IrExpression): IrExpression {
                    if (expression.type.shouldExplicitlyConvertToString)
                        return expression.explicitlyConvertedToString()
                    return expression
                }
            })
        return super.visitStringConcatenation(expression)
    }
}
