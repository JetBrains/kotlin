/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.WasmIrArithBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.ir.ArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.BaseTypeOperatorTransformer
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

class WasmTypeOperatorLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val symbols = context.wasmSymbols

    val wasmCalculator = WasmIrArithBuilder(context)

    private val isInterfaceSymbol get() = context.wasmSymbols.isInterface

    private val instanceOfIntrinsicSymbol = context.wasmSymbols.isSubClass

    private val litTrue: IrExpression get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
    private val litFalse: IrExpression get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false)

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : BaseTypeOperatorTransformer(context) {
            override val calculator: ArithBuilder = wasmCalculator

            override fun advancedCheckRequired(type: IrType): Boolean = true

            override fun lowerIntegerCoercion(expression: IrTypeOperatorCall, declaration: IrDeclarationParent): IrExpression {
                TODO("supper integer coercion")
            }

            override fun generateTypeCheckNonNull(argument: IrExpressionWithCopy, toType: IrType): IrExpression {
                assert(!toType.isMarkedNullable())
                return when {
                    toType.isNothing() -> JsIrBuilder.buildComposite(context.irBuiltIns.booleanType, listOf(argument, litFalse))
                    toType.isTypeParameter() -> generateTypeCheckWithTypeParameter(argument, toType)
                    toType.isInterface() -> generateInterfaceCheck(argument, toType)
                    else -> generateNativeInstanceOf(argument, toType)
                }
            }

            override fun lowerCast(
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


            private fun generateTypeCheckWithTypeParameter(argument: IrExpressionWithCopy, toType: IrType): IrExpression {
                val typeParameterSymbol =
                    (toType.classifierOrNull as? IrTypeParameterSymbol) ?: error("expected type parameter, but $toType")

                val typeParameter = typeParameterSymbol.owner

                return typeParameter.superTypes.fold(litTrue) { r, t ->
                    val check = generateTypeCheckNonNull(argument.copy(), t.makeNotNull())
                    wasmCalculator.and(r, check)
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

            override fun lowerCoercionToUnit(expression: IrTypeOperatorCall): IrExpression {
                return expression.argument
            }
        }, irFile)
    }
}