/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.isFunctionOrKFunctionType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.JsSymbolBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.*

class TypeOperatorLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val unit = context.builtIns.unit
    private val unitValue = JsIrBuilder.buildGetObjectValue(unit.defaultType, context.symbolTable.referenceClass(unit))

    private val lit24 = JsIrBuilder.buildInt(context.builtIns.intType, 24)
    private val lit16 = JsIrBuilder.buildInt(context.builtIns.intType, 16)

    private val byteMask = JsIrBuilder.buildInt(context.builtIns.intType, 0xFF)
    private val shortMask = JsIrBuilder.buildInt(context.builtIns.intType, 0xFFFF)

    private val calculator = JsIrArithBuilder(context)

    //NOTE: Should we define JS-own functions similar to current implementation?
    private val throwCCE = context.irBuiltIns.throwCceSymbol
    private val throwNPE = context.irBuiltIns.throwNpeSymbol

    private val eqeq = context.irBuiltIns.eqeqSymbol

    private val isInterfaceSymbol = getInternalFunction("isInterface")
    private val isArraySymbol = getInternalFunction("isArray")
    private val isCharSymbol = getInternalFunction("isChar")
    private val isObjectSymbol = getInternalFunction("isObject")

    private val instanceOfIntrinsicSymbol = context.intrinsics.jsInstanceOf.symbol
    private val typeOfIntrinsicSymbol = context.intrinsics.jsTypeOf.symbol
    private val toJSTypeIntrinsicSymbol = context.intrinsics.jsToJsType.symbol

    private val stringMarker = JsIrBuilder.buildString(context.irBuiltIns.string, "string")
    private val booleanMarker = JsIrBuilder.buildString(context.irBuiltIns.string, "boolean")
    private val functionMarker = JsIrBuilder.buildString(context.irBuiltIns.string, "function")
    private val numberMarker = JsIrBuilder.buildString(context.irBuiltIns.string, "number")

    private val litTrue: IrExpression = JsIrBuilder.buildBoolean(context.irBuiltIns.bool, true)
    private val litNull: IrExpression = JsIrBuilder.buildNull(context.builtIns.nullableNothingType)

    private fun getInternalFunction(name: String) = context.symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())

    override fun lower(irFile: IrFile) {
        // TODO: get rid of descriptors
        irFile.transformChildren(object : IrElementTransformer<DeclarationDescriptor> {
            override fun visitDeclaration(declaration: IrDeclaration, data: DeclarationDescriptor) =
                super.visitDeclaration(declaration, declaration.descriptor)

            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: DeclarationDescriptor): IrExpression {
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

            private fun lowerImplicitNotNull(expression: IrTypeOperatorCall, containingDeclaration: DeclarationDescriptor): IrExpression {
                assert(expression.operator == IrTypeOperator.IMPLICIT_NOTNULL)
                assert(expression.typeOperand.isNullable() xor expression.argument.type.isNullable())

                val newStatements = mutableListOf<IrStatement>()

                val argument = cacheValue(expression.argument, newStatements, containingDeclaration)
                val irNullCheck = nullCheck(argument)

                newStatements += JsIrBuilder.buildIfElse(expression.typeOperand, irNullCheck, JsIrBuilder.buildCall(throwNPE), argument)

                return expression.run { IrCompositeImpl(startOffset, endOffset, typeOperand, null, newStatements) }
            }

            private fun lowerCast(
                expression: IrTypeOperatorCall,
                containingDeclaration: DeclarationDescriptor,
                isSafe: Boolean
            ): IrExpression {
                assert(expression.operator == IrTypeOperator.CAST || expression.operator == IrTypeOperator.SAFE_CAST)
                assert((expression.operator == IrTypeOperator.SAFE_CAST) == isSafe)

                val toType = expression.typeOperand
                val failResult = if (isSafe) litNull else JsIrBuilder.buildCall(throwCCE)

                val newStatements = mutableListOf<IrStatement>()

                val argument = cacheValue(expression.argument, newStatements, containingDeclaration)

                val check = generateTypeCheck(argument, toType)

                newStatements += JsIrBuilder.buildIfElse(toType, check, argument, failResult)

                return expression.run { IrCompositeImpl(startOffset, endOffset, toType, null, newStatements) }
            }

            private fun lowerImplicitCast(expression: IrTypeOperatorCall) = expression.run {
                assert(operator == IrTypeOperator.IMPLICIT_CAST)
                IrCompositeImpl(startOffset, endOffset, typeOperand, null, listOf(argument))
            }

            // Note: native `instanceOf` is not used which is important because of null-behaviour
            private fun advancedCheckRequired(type: KotlinType) = type.isInterface() ||
                    KotlinBuiltIns.isArray(type) ||
                    KotlinBuiltIns.isPrimitiveArray(type) ||
                    isTypeOfCheckingType(type)

            private fun isTypeOfCheckingType(type: KotlinType) =
                ((type.isPrimitiveNumberType() || KotlinBuiltIns.isNumber(type)) && !type.isLong() && !type.isChar()) ||
                        type.isBoolean() ||
                        type.isFunctionOrKFunctionType ||
                        KotlinBuiltIns.isString(type)

            fun lowerInstanceOf(
                expression: IrTypeOperatorCall,
                containingDeclaration: DeclarationDescriptor,
                inverted: Boolean
            ): IrExpression {
                assert(expression.operator == IrTypeOperator.INSTANCEOF || expression.operator == IrTypeOperator.NOT_INSTANCEOF)
                assert((expression.operator == IrTypeOperator.NOT_INSTANCEOF) == inverted)

                val toType = expression.typeOperand
                val isCopyRequired = expression.argument.type.isNullable() && advancedCheckRequired(toType.makeNotNullable())
                val newStatements = mutableListOf<IrStatement>()

                val argument =
                    if (isCopyRequired) cacheValue(expression.argument, newStatements, containingDeclaration) else expression.argument
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

            private fun cacheValue(value: IrExpression, newStatements: MutableList<IrStatement>, cd: DeclarationDescriptor): IrExpression {
                val varSymbol = JsSymbolBuilder.buildTempVar(cd, value.type, mutable = false)
                newStatements += JsIrBuilder.buildVar(varSymbol, value)
                return JsIrBuilder.buildGetValue(varSymbol)
            }

            private fun generateTypeCheck(argument: IrExpression, toType: KotlinType): IrExpression {
                val toNotNullable = toType.makeNotNullable()
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

            private fun generateTypeCheckNonNull(argument: IrExpression, toType: KotlinType): IrExpression {
                assert(!toType.isMarkedNullable)
                return when {
                    KotlinBuiltIns.isAny(toType) -> generateIsObjectCheck(argument)
                    isTypeOfCheckingType(toType) -> generateTypeOfCheck(argument, toType)
                    toType.isChar() -> generateCheckForChar(argument)
                    KotlinBuiltIns.isArray(toType) -> generateGenericArrayCheck(argument)
                    KotlinBuiltIns.isPrimitiveArray(toType) -> generatePrimitiveArrayTypeCheck(argument, toType)
                    toType.isTypeParameter() -> generateTypeCheckWithTypeParameter(argument, toType)
                    toType.isInterface() -> generateInterfaceCheck(argument, toType)
                    else -> generateNativeInstanceOf(argument, toType)
                }
            }

            private fun generateIsObjectCheck(argument: IrExpression) = JsIrBuilder.buildCall(isObjectSymbol).apply {
                putValueArgument(0, argument)
            }

            private fun generateTypeCheckWithTypeParameter(argument: IrExpression, toType: KotlinType): IrExpression {
                val typeParameterDescriptor = toType.constructor.declarationDescriptor as TypeParameterDescriptor
                assert(!typeParameterDescriptor.isReified) { "reified parameters have to be lowered before" }

                return typeParameterDescriptor.upperBounds.fold(litTrue) { r, t ->
                    val check = generateTypeCheckNonNull(argument, t.makeNotNullable())
                    calculator.and(r, check)
                }
            }

            private fun generateCheckForChar(argument: IrExpression) =
                JsIrBuilder.buildCall(isCharSymbol).apply { dispatchReceiver = argument }

            private fun generateTypeOfCheck(argument: IrExpression, toType: KotlinType): IrExpression {
                val marker = when {
                    toType.isFunctionOrKFunctionType -> functionMarker
                    toType.isBoolean() -> booleanMarker
                    KotlinBuiltIns.isString(toType) -> stringMarker
                    else -> numberMarker
                }

                val typeOf = JsIrBuilder.buildCall(typeOfIntrinsicSymbol).apply { putValueArgument(0, argument) }
                return JsIrBuilder.buildCall(eqeq).apply {
                    putValueArgument(0, typeOf)
                    putValueArgument(1, marker)
                }
            }

            private fun wrapTypeReference(toType: KotlinType) =
                JsIrBuilder.buildCall(toJSTypeIntrinsicSymbol).apply { putTypeArgument(0, toType) }

            private fun generateGenericArrayCheck(argument: IrExpression) =
                JsIrBuilder.buildCall(isArraySymbol).apply { putValueArgument(0, argument) }

            private fun generatePrimitiveArrayTypeCheck(argument: IrExpression, toType: KotlinType): IrExpression {
                TODO("Implement Typed Array check")
            }

            private fun generateInterfaceCheck(argument: IrExpression, toType: KotlinType): IrExpression {
                val irType = wrapTypeReference(toType)
                return JsIrBuilder.buildCall(isInterfaceSymbol).apply {
                    putValueArgument(0, argument)
                    putValueArgument(1, irType)
                }
            }

            private fun generateNativeInstanceOf(argument: IrExpression, toType: KotlinType): IrExpression {
                val irType = wrapTypeReference(toType)
                return JsIrBuilder.buildCall(instanceOfIntrinsicSymbol).apply {
                    putValueArgument(0, argument)
                    putValueArgument(1, irType)
                }
            }

            private fun lowerCoercionToUnit(expression: IrTypeOperatorCall): IrExpression {
                assert(expression.operator === IrTypeOperator.IMPLICIT_COERCION_TO_UNIT)
                return expression.run { IrCompositeImpl(startOffset, endOffset, unit.defaultType, null, listOf(argument, unitValue)) }
            }

            private fun lowerIntegerCoercion(expression: IrTypeOperatorCall, containingDeclaration: DeclarationDescriptor): IrExpression {
                assert(expression.operator === IrTypeOperator.IMPLICIT_INTEGER_COERCION)
                assert(KotlinBuiltIns.isInt(expression.argument.type))

                val isNullable = expression.argument.type.isNullable()
                val toType = expression.typeOperand

                fun maskOp(arg: IrExpression, mask: IrExpression, shift: IrExpression) = calculator.run {
                    shr(shl(and(arg, mask), shift), shift)
                }

                val newStatements = mutableListOf<IrStatement>()

                val argument =
                    if (isNullable) cacheValue(expression.argument, newStatements, containingDeclaration) else expression.argument

                val casted = when {
                    KotlinBuiltIns.isByte(toType) -> maskOp(argument, byteMask, lit24)
                    KotlinBuiltIns.isShort(toType) -> maskOp(argument, shortMask, lit16)
                    KotlinBuiltIns.isLong(toType) -> TODO("Long coercion")
                    else -> error("Unreachable execution (coercion to non-Integer type")
                }

                newStatements += if (isNullable) JsIrBuilder.buildIfElse(toType, nullCheck(argument), litNull, casted) else casted

                return expression.run { IrCompositeImpl(startOffset, endOffset, toType, null, newStatements) }
            }
        }, irFile.packageFragmentDescriptor)
    }
}