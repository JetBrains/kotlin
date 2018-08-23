/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.utils.getPrimitiveArrayElementType
import org.jetbrains.kotlin.backend.common.utils.isPrimitiveArray
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

class TypeOperatorLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val unit = context.irBuiltIns.unitType
    private val unitValue = JsIrBuilder.buildGetObjectValue(unit, unit.classifierOrFail as IrClassSymbol)

    private val lit24 = JsIrBuilder.buildInt(context.irBuiltIns.intType, 24)
    private val lit16 = JsIrBuilder.buildInt(context.irBuiltIns.intType, 16)

    private val byteMask = JsIrBuilder.buildInt(context.irBuiltIns.intType, 0xFF)
    private val shortMask = JsIrBuilder.buildInt(context.irBuiltIns.intType, 0xFFFF)

    private val calculator = JsIrArithBuilder(context)

    //NOTE: Should we define JS-own functions similar to current implementation?
    private val throwCCE = context.irBuiltIns.throwCceSymbol
    private val throwNPE = context.irBuiltIns.throwNpeSymbol

    private val eqeq = context.irBuiltIns.eqeqSymbol

    private val isInterfaceSymbol get() = context.intrinsics.isInterfaceSymbol
    private val isArraySymbol get() = context.intrinsics.isArraySymbol
    //    private val isCharSymbol get() = context.intrinsics.isCharSymbol
    private val isObjectSymbol get() = context.intrinsics.isObjectSymbol

    private val instanceOfIntrinsicSymbol = context.intrinsics.jsInstanceOf.symbol
    private val typeOfIntrinsicSymbol = context.intrinsics.jsTypeOf.symbol
    private val jsClassIntrinsicSymbol = context.intrinsics.jsClass

    private val stringMarker = JsIrBuilder.buildString(context.irBuiltIns.stringType, "string")
    private val booleanMarker = JsIrBuilder.buildString(context.irBuiltIns.stringType, "boolean")
    private val functionMarker = JsIrBuilder.buildString(context.irBuiltIns.stringType, "function")
    private val numberMarker = JsIrBuilder.buildString(context.irBuiltIns.stringType, "number")

    private val litTrue: IrExpression = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
    private val litNull: IrExpression = JsIrBuilder.buildNull(context.irBuiltIns.nothingNType)

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : IrElementTransformer<IrDeclarationParent> {
            override fun visitFunction(declaration: IrFunction, data: IrDeclarationParent) =
                super.visitFunction(declaration, declaration)

            override fun visitClass(declaration: IrClass, data: IrDeclarationParent): IrStatement {
                return super.visitClass(declaration, declaration)
            }

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
                }
            }

            private fun lowerImplicitNotNull(expression: IrTypeOperatorCall, declaration: IrDeclarationParent): IrExpression {
                assert(expression.operator == IrTypeOperator.IMPLICIT_NOTNULL)
                assert(expression.typeOperand.isNullable() xor expression.argument.type.isNullable())

                val newStatements = mutableListOf<IrStatement>()

                val argument = cacheValue(expression.argument, newStatements, declaration)
                val irNullCheck = nullCheck(argument)

                newStatements += JsIrBuilder.buildIfElse(expression.typeOperand, irNullCheck, JsIrBuilder.buildCall(throwNPE), argument)

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

                newStatements += JsIrBuilder.buildIfElse(toType, check, argument, failResult)

                return expression.run { IrCompositeImpl(startOffset, endOffset, toType, null, newStatements) }
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
                        type.isNumber() ||
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
                val isCopyRequired = expression.argument.type.isNullable() && advancedCheckRequired(toType.makeNotNull())
                val newStatements = mutableListOf<IrStatement>()

                val argument =
                    if (isCopyRequired) cacheValue(expression.argument, newStatements, declaration) else expression.argument
                val check = generateTypeCheck(argument, toType)
                val result = if (inverted) calculator.not(check) else check

                return if (isCopyRequired) {
                    newStatements += result
                    IrCompositeImpl(expression.startOffset, expression.endOffset, toType, null, newStatements)
                } else result
            }

            private fun nullCheck(value: IrExpression) = JsIrBuilder.buildCall(eqeq).apply {
                putValueArgument(0, value)
                putValueArgument(1, litNull)
            }

            private fun cacheValue(
                value: IrExpression,
                newStatements: MutableList<IrStatement>,
                declaration: IrDeclarationParent
            ): IrExpression {
                val varDeclaration = JsIrBuilder.buildVar(value.type, declaration, initializer = value)
                newStatements += varDeclaration
                return JsIrBuilder.buildGetValue(varDeclaration.symbol)
            }

            private fun generateTypeCheck(argument: IrExpression, toType: IrType): IrExpression {

                // TODO: Fix unbound symbols (in inline)
                toType.classifierOrNull?.apply {
                    if (!isBound) {
                        return argument
                    }
                }

                val toNotNullable = toType.makeNotNull()
                val instanceCheck = generateTypeCheckNonNull(argument, toNotNullable)
                val isFromNullable = argument.type.isNullable()
                val isToNullable = toType.isNullable()
                val isNativeCheck = !advancedCheckRequired(toNotNullable)

                return when {
                    !isFromNullable -> instanceCheck // ! -> *
                    isToNullable -> calculator.run { oror(nullCheck(argument), instanceCheck) } // * -> ?
                    else -> if (isNativeCheck) instanceCheck else calculator.run {
                        andand(
                            not(nullCheck(argument)),
                            instanceCheck
                        )
                    } // ? -> !
                }
            }

            private fun generateTypeCheckNonNull(argument: IrExpression, toType: IrType): IrExpression {
                assert(!toType.isMarkedNullable())
                return when {
                    toType.isAny() -> generateIsObjectCheck(argument)
                    isTypeOfCheckingType(toType) -> generateTypeOfCheck(argument, toType)
//                    toType.isChar() -> generateCheckForChar(argument)
                    toType.isArray() -> generateGenericArrayCheck(argument)
                    toType.isPrimitiveArray() -> generatePrimitiveArrayTypeCheck(argument, toType)
                    toType.isTypeParameter() -> generateTypeCheckWithTypeParameter(argument, toType)
                    toType.isInterface() -> generateInterfaceCheck(argument, toType)
                    else -> generateNativeInstanceOf(argument, toType)
                }
            }

            private fun generateIsObjectCheck(argument: IrExpression) = JsIrBuilder.buildCall(isObjectSymbol).apply {
                putValueArgument(0, argument)
            }

            private fun generateTypeCheckWithTypeParameter(argument: IrExpression, toType: IrType): IrExpression {
                val typeParameterSymbol =
                    (toType.classifierOrNull as? IrTypeParameterSymbol) ?: error("expected type parameter, but $toType")

                // TODO: Stop creating unbound symbols inline:
                // DeepCopyIrTreeWithDescriptors.copy() -> ... -> ClassifierDescriptor.getSymbol()
                if (!typeParameterSymbol.isBound) {
                    return argument
                }
                val typeParameter = typeParameterSymbol.owner

                assert(!typeParameter.isReified) { "reified parameters have to be lowered before" }

                return typeParameter.superTypes.fold(litTrue) { r, t ->
                    val check = generateTypeCheckNonNull(argument, t.makeNotNull())
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

                fun maskOp(arg: IrExpression, mask: IrExpression, shift: IrExpression) = calculator.run {
                    shr(shl(and(arg, mask), shift), shift)
                }

                val newStatements = mutableListOf<IrStatement>()

                val argument =
                    if (isNullable) cacheValue(expression.argument, newStatements, declaration) else expression.argument

                val casted = when {
                    toType.isByte() -> maskOp(argument, byteMask, lit24)
                    toType.isShort() -> maskOp(argument, shortMask, lit16)
                    toType.isLong() -> TODO("Long coercion")
                    else -> error("Unreachable execution (coercion to non-Integer type")
                }

                newStatements += if (isNullable) JsIrBuilder.buildIfElse(toType, nullCheck(argument), litNull, casted) else casted

                return expression.run { IrCompositeImpl(startOffset, endOffset, toType, null, newStatements) }
            }
        }, irFile)
    }
}