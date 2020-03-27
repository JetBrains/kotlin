/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val flattenStringConcatenationPhase = makeIrFilePhase(
    ::FlattenStringConcatenationLowering,
    name = "FlattenStringConcatenationLowering",
    description = "Flatten nested string concatenation expressions into a single IrStringConcatenation"
)

/**
 * Flattens nested string concatenation expressions into a single [IrStringConcatenation]. Consolidating these into IrStringConcatenations
 * allows the backend to produce efficient code for string concatenations (e.g., using StringBuilder for JVM).
 *
 * Example expression:
 *
 *   val s = "1" + 2 + ("s1: '$s1'" + 3.0 + null)
 *
 * IR before lowering:
 *
 *   VAR name:s type:kotlin.String flags:val
 *     CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *       $this: CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *         $this: CONST String type=kotlin.String value="1"
 *         other: CONST Int type=kotlin.Int value=2
 *       other: CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *         $this: CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *           $this: STRING_CONCATENATION type=kotlin.String
 *             CONST String type=kotlin.String value="s1: '"
 *             GET_VAR 's1: String' type=kotlin.String origin=null
 *             CONST String type=kotlin.String value="'"
 *           other: CONST Double type=kotlin.Double value=3.0
 *         other: CONST Null type=kotlin.Nothing? value=null
 *
 * IR after lowering:
 *
 *   VAR name:s type:kotlin.String flags:val
 *     STRING_CONCATENATION type=kotlin.String
 *       CONST String type=kotlin.String value="1"
 *       CONST Int type=kotlin.Int value=2
 *       CONST String type=kotlin.String value="s1: '"
 *       GET_VAR 's1: String' type=kotlin.String origin=null
 *       CONST String type=kotlin.String value="'"
 *       CONST Double type=kotlin.Double value=3.0
 *       CONST Null type=kotlin.Nothing? value=null
 */
class FlattenStringConcatenationLowering(val context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoid() {

    companion object {
        // There are two versions of String.plus in the library. One for nullable and one for non-nullable strings.
        // The version for nullable strings has FqName kotlin.plus, the version for non-nullable strings
        // is a member function of kotlin.String (with FqName kotlin.String.plus)
        private val PARENT_NAMES = setOf(
            KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME,
            KotlinBuiltIns.FQ_NAMES.string.toSafe()
        )

        /** @return true if the given expression is a call to [String.plus] */
        private val IrCall.isStringPlusCall: Boolean
            get() {
                val function = symbol.owner
                val receiver = dispatchReceiver ?: extensionReceiver

                return receiver != null
                        && receiver.type.isStringClassType()
                        && function.returnType.isStringClassType()
                        && function.valueParameters.size == 1
                        && function.name.asString() == "plus"
                        && function.fqNameWhenAvailable?.parent() in PARENT_NAMES
            }

        /** @return true if the function is Any.toString or an override of Any.toString */
        val IrSimpleFunction.isToString: Boolean
            get() {
                if (name.asString() != "toString" || valueParameters.size != 0 || !returnType.isString())
                    return false

                return (dispatchReceiverParameter != null && extensionReceiverParameter == null
                        && (dispatchReceiverParameter?.type?.isAny() == true || this.overriddenSymbols.isNotEmpty()))
            }

        /** @return true if the function is Any?.toString */
        private val IrSimpleFunction.isNullableToString: Boolean
            get() {
                if (name.asString() != "toString" || valueParameters.size != 0 || !returnType.isString())
                    return false

                return dispatchReceiverParameter == null
                        && extensionReceiverParameter?.type?.isNullableAny() == true
                        && fqNameWhenAvailable?.parent() == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
            }

        /** @return true if the given expression is a call to [toString] */
        private val IrCall.isToStringCall: Boolean
            get() {
                if (superQualifierSymbol != null)
                    return false

                val function = symbol.owner as? IrSimpleFunction
                    ?: return false

                return function.isToString || function.isNullableToString
            }

        /** @return true if the given expression is a call to [Any?.toString] or a call of [toString] on a primitive type. */
        private val IrCall.isSpecialToStringCall: Boolean
            get() = isToStringCall && dispatchReceiver?.type?.isPrimitiveType() != false

        /** @return true if the given expression is a [IrStringConcatenation], or an [IrCall] to [String.plus]. */
        private fun isStringConcatenationExpression(expression: IrExpression): Boolean =
            (expression is IrStringConcatenation) || (expression is IrCall) && expression.isStringPlusCall

        /** Recursively collects string concatenation arguments from the given expression. */
        private fun collectStringConcatenationArguments(expression: IrExpression): List<IrExpression> {
            val arguments = mutableListOf<IrExpression>()
            expression.acceptChildrenVoid(object : IrElementVisitorVoid {

                override fun visitElement(element: IrElement) {
                    // Theoretically this is unreachable code since all descendants of IrExpressions are IrExpressions.
                    element.acceptChildrenVoid(this)
                }

                override fun visitCall(expression: IrCall) {
                    if (isStringConcatenationExpression(expression) || expression.isToStringCall) {
                        // Recursively collect from call arguments.
                        expression.acceptChildrenVoid(this)
                    } else {
                        // Add call itself as an argument.
                        arguments.add(expression)
                    }
                }

                override fun visitStringConcatenation(expression: IrStringConcatenation) {
                    // Recursively collect from concatenation arguments.
                    expression.acceptChildrenVoid(this)
                }

                override fun visitExpression(expression: IrExpression) {
                    // These IrExpressions are neither IrCalls nor IrStringConcatenations and should be added as an argument.
                    arguments.add(expression)
                }
            })

            return arguments
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitExpression(expression: IrExpression): IrExpression {
        // Only modify/flatten string concatenation expressions.
        val transformedExpression =
            if (isStringConcatenationExpression(expression) || expression is IrCall && expression.isSpecialToStringCall)
                expression.run {
                    IrStringConcatenationImpl(
                        startOffset,
                        endOffset,
                        type,
                        collectStringConcatenationArguments(this)
                    )
                }
            else expression

        transformedExpression.transformChildrenVoid(this)
        return transformedExpression
    }
}
