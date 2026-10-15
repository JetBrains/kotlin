/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isTopLevelInPackage
import org.jetbrains.kotlin.ir.util.isUnsigned
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.math.max
import kotlin.math.min

/**
 * Flattens nested string concatenation expressions into a single [IrStringConcatenation]. Consolidating these into IrStringConcatenations
 * allows the backend to produce efficient code for string concatenations (e.g., using StringBuilder for JVM).
 *
 * Example expression:
 *
 * ```kotlin
 * val s = "1" + 2 + ("s1: '$s1'" + 3.0 + null)
 * ```
 *
 * IR before lowering:
 *
 * ```
 * VAR name:s type:kotlin.String flags:val
 *   CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *     $this: CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *       $this: CONST String type=kotlin.String value="1"
 *       other: CONST Int type=kotlin.Int value=2
 *     other: CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *       $this: CALL 'plus(Any?): String' type=kotlin.String origin=PLUS
 *         $this: STRING_CONCATENATION type=kotlin.String
 *           CONST String type=kotlin.String value="s1: '"
 *           GET_VAR 's1: String' type=kotlin.String origin=null
 *           CONST String type=kotlin.String value="'"
 *         other: CONST Double type=kotlin.Double value=3.0
 *       other: CONST Null type=kotlin.Nothing? value=null
 * ```
 *
 * IR after lowering:
 *
 * ```
 * VAR name:s type:kotlin.String flags:val
 *   STRING_CONCATENATION type=kotlin.String
 *     CONST String type=kotlin.String value="1"
 *     CONST Int type=kotlin.Int value=2
 *     CONST String type=kotlin.String value="s1: '"
 *     GET_VAR 's1: String' type=kotlin.String origin=null
 *     CONST String type=kotlin.String value="'"
 *     CONST Double type=kotlin.Double value=3.0
 *     CONST Null type=kotlin.Nothing? value=null
 * ```
 */
class FlattenStringConcatenationLowering(val context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    companion object {
        // There are two versions of String.plus in the library. One for nullable and one for non-nullable strings.
        // The version for nullable strings has FqName kotlin.plus, the version for non-nullable strings
        // is a member function of kotlin.String (with FqName kotlin.String.plus)
        private val PARENT_NAMES = setOf(
            StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
            StandardNames.FqNames.string.toSafe()
        )

        /** @return true if the given expression is a call to [String.plus] */
        private val IrCall.isStringPlusCall: Boolean
            get() {
                val function = symbol.owner
                return (function.hasShape(dispatchReceiver = true, regularParameters = 1)
                        || function.hasShape(extensionReceiver = true, regularParameters = 1))
                        && function.parameters[0].type.isStringClassType()
                        && function.returnType.isStringClassType()
                        && function.name == OperatorNameConventions.PLUS
                        && function.fqNameWhenAvailable?.parent() in PARENT_NAMES
            }

        /** @return true if the function is Any.toString or an override of Any.toString */
        val IrSimpleFunction.isToString: Boolean
            get() = name == OperatorNameConventions.TO_STRING
                    && hasShape(dispatchReceiver = true)
                    && returnType.isString()
                    && (parameters[0].type.isAny() || overriddenSymbols.isNotEmpty())

        /** @return true if the function is Any?.toString */
        private val IrSimpleFunction.isNullableToString: Boolean
            get() = isTopLevelInPackage(OperatorNameConventions.TO_STRING.asString(), StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
                    && returnType.isString()
                    && hasShape(extensionReceiver = true)
                    && parameters[0].type.isNullableAny()

        /** @return true if the given expression is a call to [toString] */
        private val IrCall.isToStringCall: Boolean
            get() {
                if (superQualifierSymbol != null)
                    return false

                val function = symbol.owner
                return function.isToString || function.isNullableToString
            }

        /** @return true if the given expression is a call to [Any?.toString] or a call of [toString] on a primitive type. */
        private val IrCall.isSpecialToStringCall: Boolean
            get() = isToStringCall && dispatchReceiver?.type?.isPrimitiveType() != false

        /** @return true if the given expression is a [IrStringConcatenation], or an [IrCall] to [String.plus]. */
        private fun isStringConcatenationExpression(expression: IrExpression): Boolean = TODO()

        /** Recursively collects string concatenation arguments from the given expression. */
        private fun collectStringConcatenationArguments(expression: IrExpression): List<IrExpression> { TODO() }
    }

    override fun lower(irFile: IrFile) { TODO() }

    override fun visitExpression(expression: IrExpression): IrExpression { TODO() }

    private fun IrStringConcatenation.tryToFold(): IrExpression { TODO() }

    private fun constToString(const: IrConst): String { TODO() }

    private fun normalizeUnsignedValue(const: IrConst): Any? { TODO() }
}
