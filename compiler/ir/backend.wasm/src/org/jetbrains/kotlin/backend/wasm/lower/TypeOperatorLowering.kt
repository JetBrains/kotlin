/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.WasmIrArithBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionWithCopy
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

class WasmTypeOperatorLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val symbols = context.wasmSymbols

    val calculator = WasmIrArithBuilder(context)

    private val throwCCE = context.ir.symbols.ThrowTypeCastException
    private val throwNPE = context.ir.symbols.ThrowNullPointerException

    private val eqeq = context.irBuiltIns.eqeqSymbol

    private fun booleanNot(e: IrExpression): IrExpression {
        return JsIrBuilder.buildCall(context.wasmSymbols.booleanNot).apply {
            putValueArgument(0, e)
        }
    }

    private val isInterfaceSymbol get() = context.wasmSymbols.isInterface

    private val instanceOfIntrinsicSymbol = context.wasmSymbols.isSubClass

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
                    IrTypeOperator.IMPLICIT_INTEGER_COERCION -> TODO("Implement integer coercion ${expression.render()}")
                    IrTypeOperator.IMPLICIT_NOTNULL -> lowerImplicitNotNull(expression, data)
                    IrTypeOperator.INSTANCEOF -> lowerInstanceOf(expression, data, false)
                    IrTypeOperator.NOT_INSTANCEOF -> lowerInstanceOf(expression, data, true)
                    IrTypeOperator.CAST -> lowerCast(expression, data, false)
                    IrTypeOperator.SAFE_CAST -> lowerCast(expression, data, true)
                    IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> error("Dynamic casts are not supported ${expression.render()}")
                    IrTypeOperator.SAM_CONVERSION -> error("SAM conversion: ${expression.render()}")
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
                val fromType = expression.argument.type

                // TODO: Support boxing/unboxing of primitive types
                if (toType.isPrimitiveType())
                    return JsIrBuilder.buildCall(throwCCE)

                if (fromType.isSubtypeOf(toType, context.irBuiltIns))
                    return expression.argument

                val failResult = if (isSafe) litNull else JsIrBuilder.buildCall(throwCCE)

                val newStatements = mutableListOf<IrStatement>()

                val argument = cacheValue(expression.argument, newStatements, declaration)
                val check = generateTypeCheck(argument, toType)

                val narrowArg = JsIrBuilder.buildCall(symbols.structNarrow).apply {
                    putTypeArgument(0, fromType)
                    putTypeArgument(1, toType)
                    putValueArgument(0, argument())
                }

                newStatements += JsIrBuilder.buildIfElse(expression.type, check, narrowArg, failResult)

                return expression.run {
                    IrCompositeImpl(startOffset, endOffset, expression.type, null, newStatements)
                }
            }

            private fun lowerImplicitCast(expression: IrTypeOperatorCall) = expression.run {
                assert(operator == IrTypeOperator.IMPLICIT_CAST)
                argument
            }

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
                val result = if (inverted) booleanNot(check) else check
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
                val toNotNullable = toType.makeNotNull()
                val argumentInstance = argument()
                val instanceCheck = generateTypeCheckNonNull(argumentInstance, toNotNullable)
                val isFromNullable = argumentInstance.type.isNullable()
                val isToNullable = toType.isNullable()

                return when {
                    !isFromNullable -> instanceCheck // ! -> *
                    isToNullable -> calculator.run { oror(nullCheck(argument()), instanceCheck) } // * -> ?
                    else -> calculator.run {
                        andand(
                            not(nullCheck(argument())),
                            instanceCheck
                        )
                    }
                }
            }

            private fun generateTypeCheckNonNull(argument: IrExpressionWithCopy, toType: IrType): IrExpression {
                assert(!toType.isMarkedNullable())
                return when {
                    toType.isNothing() -> JsIrBuilder.buildComposite(context.irBuiltIns.booleanType, listOf(argument, litFalse))
                    toType.isTypeParameter() -> generateTypeCheckWithTypeParameter(argument, toType)
                    toType.isInterface() -> generateInterfaceCheck(argument, toType)
                    else -> generateNativeInstanceOf(argument, toType)
                }
            }

            private fun generateTypeCheckWithTypeParameter(argument: IrExpressionWithCopy, toType: IrType): IrExpression {
                val typeParameterSymbol =
                    (toType.classifierOrNull as? IrTypeParameterSymbol) ?: error("expected type parameter, but $toType")

                val typeParameter = typeParameterSymbol.owner

                return typeParameter.superTypes.fold(litTrue) { r, t ->
                    val check = generateTypeCheckNonNull(argument.copy(), t.makeNotNull())
                    calculator.and(r, check)
                }
            }

            private fun getClassId(toType: IrType) =
                JsIrBuilder.buildCall(symbols.wasmClassId).apply { putTypeArgument(0, toType) }

            private fun getInterfaceId(toType: IrType) =
                JsIrBuilder.buildCall(symbols.wasmInterfaceId).apply { putTypeArgument(0, toType) }

            private fun generateInterfaceCheck(argument: IrExpression, toType: IrType): IrExpression {
                val irType = getInterfaceId(toType)
                return JsIrBuilder.buildCall(isInterfaceSymbol).apply {
                    putValueArgument(0, argument)
                    putValueArgument(1, irType)
                }
            }

            private fun generateNativeInstanceOf(argument: IrExpression, toType: IrType): IrExpression {
                val irType = getClassId(toType)
                return JsIrBuilder.buildCall(instanceOfIntrinsicSymbol).apply {
                    putValueArgument(0, argument)
                    putValueArgument(1, irType)
                }
            }

            private fun lowerCoercionToUnit(expression: IrTypeOperatorCall): IrExpression {
                return expression.argument
            }
        }, irFile)
    }
}