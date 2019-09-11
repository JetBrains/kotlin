/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.ArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionWithCopy
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

class TypeOperatorLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val jsIntrinsics = context.intrinsics
    private val unit = context.irBuiltIns.unitType

    private val lit24 get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 24)
    private val lit16 get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 16)

    private val byteMask get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 0xFF)
    private val shortMask get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 0xFFFF)

    private val jsCalculator = JsIrArithBuilder(context)

    private val isInterfaceSymbol get() = context.intrinsics.isInterfaceSymbol
    private val isArraySymbol get() = context.intrinsics.isArraySymbol
    private val isSuspendFunctionSymbol = context.intrinsics.isSuspendFunctionSymbol
    //    private val isCharSymbol get() = context.intrinsics.isCharSymbol
    private val isObjectSymbol get() = context.intrinsics.isObjectSymbol

    private val instanceOfIntrinsicSymbol = context.intrinsics.jsInstanceOf
    private val typeOfIntrinsicSymbol = context.intrinsics.jsTypeOf
    private val jsClassIntrinsicSymbol = context.intrinsics.jsClass

    private val stringMarker get() = JsIrBuilder.buildString(context.irBuiltIns.stringType, "string")
    private val booleanMarker get() = JsIrBuilder.buildString(context.irBuiltIns.stringType, "boolean")
    private val functionMarker get() = JsIrBuilder.buildString(context.irBuiltIns.stringType, "function")
    private val numberMarker get() = JsIrBuilder.buildString(context.irBuiltIns.stringType, "number")

    private val litTrue: IrExpression get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
    private val litFalse: IrExpression get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false)

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : BaseTypeOperatorTransformer(context) {
            override val calculator: ArithBuilder = jsCalculator

            override fun lowerCast(
                expression: IrTypeOperatorCall,
                declaration: IrDeclarationParent,
                isSafe: Boolean
            ): IrExpression {
                assert(expression.operator == IrTypeOperator.CAST || expression.operator == IrTypeOperator.SAFE_CAST)
                assert((expression.operator == IrTypeOperator.SAFE_CAST) == isSafe)

                val toType = expression.typeOperand
                val failResult = if (isSafe) litNull else JsIrBuilder.buildCall(throwCCE)

                val newStatements = mutableListOf<IrStatement>()

                val argument = cacheValue(expression.argument, newStatements, declaration)
                val check = generateTypeCheck(argument, toType)

                newStatements += JsIrBuilder.buildIfElse(expression.type, check, argument(), failResult)

                return expression.run {
                    IrCompositeImpl(startOffset, endOffset, expression.type, null, newStatements)
                }
            }

            // Note: native `instanceOf` is not used which is important because of null-behaviour
            override fun advancedCheckRequired(type: IrType) = type.isInterface() ||
                    type.isArray() ||
                    type.isPrimitiveArray() ||
                    isTypeOfCheckingType(type)

            private fun isTypeOfCheckingType(type: IrType) =
                type.isByte() ||
                        type.isShort() ||
                        type.isInt() ||
                        type.isFloat() ||
                        type.isDouble() ||
                        type.isBoolean() ||
                        type.isFunctionOrKFunction() ||
                        type.isString()


            override fun generateTypeCheckNonNull(argument: IrExpressionWithCopy, toType: IrType): IrExpression {
                assert(!toType.isMarkedNullable())
                return when {
                    toType.isAny() -> generateIsObjectCheck(argument)
                    toType.isNothing() -> JsIrBuilder.buildComposite(context.irBuiltIns.booleanType, listOf(argument, litFalse))
                    toType.isSuspendFunctionTypeOrSubtype() -> generateSuspendFunctionCheck(argument, toType)
                    isTypeOfCheckingType(toType) -> generateTypeOfCheck(argument, toType)
//                    toType.isChar() -> generateCheckForChar(argument)
                    toType.isNumber() -> generateNumberCheck(argument)
                    toType.isComparable() -> generateComparableCheck(argument)
                    toType.isCharSequence() -> generateCharSequenceCheck(argument)
                    toType.isArray() -> generateGenericArrayCheck(argument)
                    toType.isPrimitiveArray() -> generatePrimitiveArrayTypeCheck(argument, toType)
                    toType.isTypeParameter() -> generateTypeCheckWithTypeParameter(argument, toType)
                    toType.isInterface() -> {
                        if ((toType.classifierOrFail.owner as IrClass).isEffectivelyExternal()) {
                            generateIsObjectCheck(argument)
                        } else {
                            generateInterfaceCheck(argument, toType)
                        }
                    }
                    else -> generateNativeInstanceOf(argument, toType)
                }
            }

            private fun generateIsObjectCheck(argument: IrExpression) = JsIrBuilder.buildCall(isObjectSymbol).apply {
                putValueArgument(0, argument)
            }

            private fun generateTypeCheckWithTypeParameter(argument: IrExpressionWithCopy, toType: IrType): IrExpression {
                val typeParameterSymbol =
                    (toType.classifierOrNull as? IrTypeParameterSymbol) ?: error("expected type parameter, but $toType")

                val typeParameter = typeParameterSymbol.owner

                // TODO either remove functions with reified type parameters or support this case
                // assert(!typeParameter.isReified) { "reified parameters have to be lowered before" }
                return typeParameter.superTypes.fold(litTrue) { r, t ->
                    val check = generateTypeCheckNonNull(argument.copy(), t.makeNotNull())
                    jsCalculator.and(r, check)
                }
            }

//            private fun generateCheckForChar(argument: IrExpression) =
//                JsIrBuilder.buildCall(isCharSymbol).apply { dispatchReceiver = argument }

            private fun generateSuspendFunctionCheck(argument: IrExpression, toType: IrType): IrExpression {
                val arity = (toType.classifierOrFail.owner as IrClass).typeParameters.size - 1 // drop return type

                val irBuiltIns = context.irBuiltIns
                return JsIrBuilder.buildCall(isSuspendFunctionSymbol, irBuiltIns.booleanType).apply {
                    putValueArgument(0, argument)
                    putValueArgument(1, JsIrBuilder.buildInt(irBuiltIns.intType, arity))
                }
            }

            private fun generateTypeOfCheck(argument: IrExpression, toType: IrType): IrExpression {
                val marker = when {
                    toType.isFunctionOrKFunction() -> functionMarker
                    toType.isBoolean() -> booleanMarker
                    toType.isString() -> stringMarker
                    else -> numberMarker
                }

                val typeOf = JsIrBuilder.buildCall(typeOfIntrinsicSymbol).apply { putValueArgument(0, argument) }
                return JsIrBuilder.buildCall(eqeq).apply {
                    putValueArgument(0, typeOf)
                    putValueArgument(1, marker)
                }
            }

            private fun wrapTypeReference(toType: IrType) =
                JsIrBuilder.buildCall(jsClassIntrinsicSymbol).apply { putTypeArgument(0, toType) }

            private fun generateGenericArrayCheck(argument: IrExpression) =
                JsIrBuilder.buildCall(isArraySymbol).apply { putValueArgument(0, argument) }

            private fun generateNumberCheck(argument: IrExpression) =
                JsIrBuilder.buildCall(jsIntrinsics.isNumberSymbol).apply { putValueArgument(0, argument) }

            private fun generateComparableCheck(argument: IrExpression) =
                JsIrBuilder.buildCall(jsIntrinsics.isComparableSymbol).apply { putValueArgument(0, argument) }

            private fun generateCharSequenceCheck(argument: IrExpression) =
                JsIrBuilder.buildCall(jsIntrinsics.isCharSequenceSymbol).apply { putValueArgument(0, argument) }

            private fun generatePrimitiveArrayTypeCheck(argument: IrExpression, toType: IrType): IrExpression {
                val f = jsIntrinsics.isPrimitiveArray[toType.getPrimitiveArrayElementType()]!!
                return JsIrBuilder.buildCall(f).apply { putValueArgument(0, argument) }
            }

            private fun generateInterfaceCheck(argument: IrExpression, toType: IrType): IrExpression {
                val irType = wrapTypeReference(toType)
                return JsIrBuilder.buildCall(isInterfaceSymbol).apply {
                    putValueArgument(0, argument)
                    putValueArgument(1, irType)
                }
            }

            private fun generateNativeInstanceOf(argument: IrExpression, toType: IrType): IrExpression {
                val irType = wrapTypeReference(toType)
                return JsIrBuilder.buildCall(instanceOfIntrinsicSymbol).apply {
                    putValueArgument(0, argument)
                    putValueArgument(1, irType)
                }
            }

            override fun lowerCoercionToUnit(expression: IrTypeOperatorCall): IrExpression {
                assert(expression.operator === IrTypeOperator.IMPLICIT_COERCION_TO_UNIT)
                return expression.run { IrCompositeImpl(startOffset, endOffset, unit, null, listOf(argument, unitValue)) }
            }

            override fun lowerIntegerCoercion(expression: IrTypeOperatorCall, declaration: IrDeclarationParent): IrExpression {
                assert(expression.operator === IrTypeOperator.IMPLICIT_INTEGER_COERCION)
                assert(expression.argument.type.isInt())

                val isNullable = expression.argument.type.isNullable()
                val toType = expression.typeOperand

                fun maskOp(arg: IrExpression, mask: IrExpression, shift: IrExpressionWithCopy) = jsCalculator.run {
                    shr(shl(and(arg, mask), shift), shift.copy())
                }

                val newStatements = mutableListOf<IrStatement>()
                val argument = cacheValue(expression.argument, newStatements, declaration)

                val casted = when {
                    toType.isByte() -> maskOp(argument(), byteMask, lit24)
                    toType.isShort() -> maskOp(argument(), shortMask, lit16)
                    toType.isLong() -> JsIrBuilder.buildCall(jsIntrinsics.jsToLong).apply {
                        putValueArgument(0, argument())
                    }
                    else -> error("Unreachable execution (coercion to non-Integer type")
                }

                newStatements += if (isNullable) JsIrBuilder.buildIfElse(toType, nullCheck(argument()), litNull, casted) else casted

                return expression.run { IrCompositeImpl(startOffset, endOffset, toType, null, newStatements) }
            }
        }, irFile)
    }
}