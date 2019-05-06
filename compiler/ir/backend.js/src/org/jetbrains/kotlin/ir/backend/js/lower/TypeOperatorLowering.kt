/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
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
    private val unit = context.irBuiltIns.unitType
    private val unitValue get() = JsIrBuilder.buildGetObjectValue(unit, unit.classifierOrFail as IrClassSymbol)

    private val lit24 get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 24)
    private val lit16 get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 16)

    private val byteMask get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 0xFF)
    private val shortMask get() = JsIrBuilder.buildInt(context.irBuiltIns.intType, 0xFFFF)

    private val calculator = JsIrArithBuilder(context)

    //NOTE: Should we define JS-own functions similar to current implementation?
    private val throwCCE = context.ir.symbols.ThrowTypeCastException
    private val throwNPE = context.ir.symbols.ThrowNullPointerException

    private val eqeq = context.irBuiltIns.eqeqSymbol

    private val isInterfaceSymbol get() = context.intrinsics.isInterfaceSymbol
    private val isArraySymbol get() = context.intrinsics.isArraySymbol
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
    private val litNull: IrExpression get() = JsIrBuilder.buildNull(context.irBuiltIns.nothingNType)

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : IrElementTransformer<IrDeclarationParent> {
            override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent) =
                super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclarationParent): IrExpression {
                super.visitTypeOperator(expression, data)

                return when (expression.operator) {
                    IrTypeOperator.IMPLICIT_CAST -> lowerImplicitCast(expression)
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> lowerCoercionToUnit(expression)
                    IrTypeOperator.IMPLICIT_INTEGER_COERCION -> lowerIntegerCoercion(expression, data)
                    IrTypeOperator.IMPLICIT_NOTNULL -> lowerImplicitNotNull(expression, data)
                    IrTypeOperator.INSTANCEOF -> lowerInstanceOf(expression, data, false)
                    IrTypeOperator.NOT_INSTANCEOF -> lowerInstanceOf(expression, data, true)
                    IrTypeOperator.CAST -> lowerCast(expression, data, false)
                    IrTypeOperator.SAFE_CAST -> lowerCast(expression, data, true)
                    IrTypeOperator.SAM_CONVERSION -> TODO("SAM conversion: ${expression.render()}")
                }
            }

            private fun lowerImplicitNotNull(expression: IrTypeOperatorCall, declaration: IrDeclarationParent): IrExpression {
                assert(expression.operator == IrTypeOperator.IMPLICIT_NOTNULL)
                assert(expression.typeOperand.isNullable() xor expression.argument.type.isNullable())

                val newStatements = mutableListOf<IrStatement>()

                val argument = cacheValue(expression.argument, newStatements, declaration)
                val irNullCheck = nullCheck(argument())

                newStatements += JsIrBuilder.buildIfElse(expression.typeOperand, irNullCheck, JsIrBuilder.buildCall(throwNPE), argument())

                return expression.run { IrCompositeImpl(startOffset, endOffset, typeOperand, null, newStatements) }
            }

            private fun lowerCast(
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

            private fun lowerImplicitCast(expression: IrTypeOperatorCall) = expression.run {
                assert(operator == IrTypeOperator.IMPLICIT_CAST)
                argument
            }

            // Note: native `instanceOf` is not used which is important because of null-behaviour
            private fun advancedCheckRequired(type: IrType) = type.isInterface() ||
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

            fun lowerInstanceOf(
                expression: IrTypeOperatorCall,
                declaration: IrDeclarationParent,
                inverted: Boolean
            ): IrExpression {
                assert(expression.operator == IrTypeOperator.INSTANCEOF || expression.operator == IrTypeOperator.NOT_INSTANCEOF)
                assert((expression.operator == IrTypeOperator.NOT_INSTANCEOF) == inverted)

                val toType = expression.typeOperand
                val newStatements = mutableListOf<IrStatement>()

                val argument = cacheValue(expression.argument, newStatements, declaration)
                val check = generateTypeCheck(argument, toType)
                val result = if (inverted) calculator.not(check) else check
                newStatements += result
                return IrCompositeImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.irBuiltIns.booleanType,
                    null,
                    newStatements
                )
            }

            private fun nullCheck(value: IrExpression) = JsIrBuilder.buildCall(eqeq).apply {
                putValueArgument(0, value)
                putValueArgument(1, litNull)
            }

            private fun cacheValue(
                value: IrExpression,
                newStatements: MutableList<IrStatement>,
                declaration: IrDeclarationParent
            ): () -> IrExpressionWithCopy {
                val varDeclaration = JsIrBuilder.buildVar(value.type, declaration, initializer = value)
                newStatements += varDeclaration
                return { JsIrBuilder.buildGetValue(varDeclaration.symbol) }
            }

            private fun generateTypeCheck(argument: () -> IrExpressionWithCopy, toType: IrType): IrExpression {

                // TODO: Fix unbound symbols (in inline)
                toType.classifierOrNull?.apply {
                    if (!isBound) {
                        return argument()
                    }
                }

                val toNotNullable = toType.makeNotNull()
                val argumentInstance = argument()
                val instanceCheck = generateTypeCheckNonNull(argumentInstance, toNotNullable)
                val isFromNullable = argumentInstance.type.isNullable()
                val isToNullable = toType.isNullable()
                val isNativeCheck = !advancedCheckRequired(toNotNullable)

                return when {
                    !isFromNullable -> instanceCheck // ! -> *
                    isToNullable -> calculator.run { oror(nullCheck(argument()), instanceCheck) } // * -> ?
                    else -> if (isNativeCheck) instanceCheck else calculator.run {
                        andand(
                            not(nullCheck(argument())),
                            instanceCheck
                        )
                    } // ? -> !
                }
            }

            private fun generateTypeCheckNonNull(argument: IrExpressionWithCopy, toType: IrType): IrExpression {
                assert(!toType.isMarkedNullable())
                return when {
                    toType.isAny() -> generateIsObjectCheck(argument)
                    toType.isNothing() -> JsIrBuilder.buildComposite(context.irBuiltIns.booleanType, listOf(argument, litFalse))
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

                // TODO: Stop creating unbound symbols inline:
                // DeepCopyIrTreeWithDescriptors.copy() -> ... -> ClassifierDescriptor.getSymbol()
                if (!typeParameterSymbol.isBound) {
                    return argument
                }
                val typeParameter = typeParameterSymbol.owner

                // TODO either remove functions with reified type parameters or support this case
                // assert(!typeParameter.isReified) { "reified parameters have to be lowered before" }
                return typeParameter.superTypes.fold(litTrue) { r, t ->
                    val check = generateTypeCheckNonNull(argument.copy(), t.makeNotNull())
                    calculator.and(r, check)
                }
            }

//            private fun generateCheckForChar(argument: IrExpression) =
//                JsIrBuilder.buildCall(isCharSymbol).apply { dispatchReceiver = argument }

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
                JsIrBuilder.buildCall(context.intrinsics.isNumberSymbol).apply { putValueArgument(0, argument) }

            private fun generateComparableCheck(argument: IrExpression) =
                JsIrBuilder.buildCall(context.intrinsics.isComparableSymbol).apply { putValueArgument(0, argument) }

            private fun generateCharSequenceCheck(argument: IrExpression) =
                JsIrBuilder.buildCall(context.intrinsics.isCharSequenceSymbol).apply { putValueArgument(0, argument) }

            private fun generatePrimitiveArrayTypeCheck(argument: IrExpression, toType: IrType): IrExpression {
                val f = context.intrinsics.isPrimitiveArray[toType.getPrimitiveArrayElementType()]!!
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

            private fun lowerCoercionToUnit(expression: IrTypeOperatorCall): IrExpression {
                assert(expression.operator === IrTypeOperator.IMPLICIT_COERCION_TO_UNIT)
                return expression.run { IrCompositeImpl(startOffset, endOffset, unit, null, listOf(argument, unitValue)) }
            }

            private fun lowerIntegerCoercion(expression: IrTypeOperatorCall, declaration: IrDeclarationParent): IrExpression {
                assert(expression.operator === IrTypeOperator.IMPLICIT_INTEGER_COERCION)
                assert(expression.argument.type.isInt())

                val isNullable = expression.argument.type.isNullable()
                val toType = expression.typeOperand

                fun maskOp(arg: IrExpression, mask: IrExpression, shift: IrExpressionWithCopy) = calculator.run {
                    shr(shl(and(arg, mask), shift), shift.copy())
                }

                val newStatements = mutableListOf<IrStatement>()
                val argument = cacheValue(expression.argument, newStatements, declaration)

                val casted = when {
                    toType.isByte() -> maskOp(argument(), byteMask, lit24)
                    toType.isShort() -> maskOp(argument(), shortMask, lit16)
                    toType.isLong() -> JsIrBuilder.buildCall(context.intrinsics.jsToLong).apply {
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