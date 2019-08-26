/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.WasmIrArithBuilder
import org.jetbrains.kotlin.backend.wasm.utils.isInlinedWasm
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isPure
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionWithCopy
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

abstract class WasmBaseTypeOperatorTransformer(val context: CommonBackendContext) : IrElementTransformer<IrDeclarationParent> {
    val unit = context.irBuiltIns.unitType
    val unitValue get() = JsIrBuilder.buildGetObjectValue(unit, unit.classifierOrFail as IrClassSymbol)

    abstract val calculator: WasmIrArithBuilder

    val throwCCE = context.ir.symbols.ThrowTypeCastException
    val throwNPE = context.ir.symbols.ThrowNullPointerException

    val eqeqeq = context.irBuiltIns.eqeqeqSymbol
    val litNull: IrExpression get() = JsIrBuilder.buildNull(context.irBuiltIns.nothingNType)

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent) =
        super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclarationParent): IrExpression {
        super.visitTypeOperator(expression, data)

        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST -> lowerImplicitCast(expression)
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> lowerImplicitDynamicCast(expression)
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> lowerCoercionToUnit(expression)
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> lowerIntegerCoercion(expression, data)
            IrTypeOperator.IMPLICIT_NOTNULL -> lowerImplicitNotNull(expression, data)
            IrTypeOperator.INSTANCEOF -> lowerInstanceOf(expression, data, false)
            IrTypeOperator.NOT_INSTANCEOF -> lowerInstanceOf(expression, data, true)
            IrTypeOperator.CAST -> lowerCast(expression, data, false)
            IrTypeOperator.SAFE_CAST -> lowerCast(expression, data, true)
            IrTypeOperator.SAM_CONVERSION -> TODO("SAM conversion: ${expression.render()}")
            IrTypeOperator.REINTERPRET_CAST -> expression
        }
    }

    fun lowerImplicitNotNull(expression: IrTypeOperatorCall, declaration: IrDeclarationParent): IrExpression {
        assert(expression.operator == IrTypeOperator.IMPLICIT_NOTNULL)
        assert(expression.typeOperand.isNullable() xor expression.argument.type.isNullable())

        val newStatements = mutableListOf<IrStatement>()

        val argument = cacheValue(expression.argument, newStatements, declaration)
        val irNullCheck = nullCheck(argument())

        newStatements += JsIrBuilder.buildIfElse(expression.typeOperand, irNullCheck, JsIrBuilder.buildCall(throwNPE), argument())

        return expression.run { IrCompositeImpl(startOffset, endOffset, typeOperand, null, newStatements) }
    }

    abstract fun lowerCast(
        expression: IrTypeOperatorCall,
        declaration: IrDeclarationParent,
        isSafe: Boolean
    ): IrExpression


    abstract fun lowerImplicitCast(expression: IrTypeOperatorCall): IrExpression

    fun lowerImplicitDynamicCast(expression: IrTypeOperatorCall) = expression.run {
        // TODO check argument
        assert(operator == IrTypeOperator.IMPLICIT_DYNAMIC_CAST)
        argument
    }

    // Note: native `instanceOf` is not used which is important because of null-behaviour
    abstract fun advancedCheckRequired(type: IrType): Boolean

    fun lowerInstanceOf(
        expression: IrTypeOperatorCall,
        declaration: IrDeclarationParent,
        inverted: Boolean
    ): IrExpression {
        assert(expression.operator == IrTypeOperator.INSTANCEOF || expression.operator == IrTypeOperator.NOT_INSTANCEOF)
        assert((expression.operator == IrTypeOperator.NOT_INSTANCEOF) == inverted)

        val fromType = expression.argument.type
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

    fun nullCheck(value: IrExpression) = JsIrBuilder.buildCall(eqeqeq).apply {
        putValueArgument(0, litNull)
        putValueArgument(1, value)
    }

    fun cacheValue(
        value: IrExpression,
        newStatements: MutableList<IrStatement>,
        declaration: IrDeclarationParent
    ): () -> IrExpressionWithCopy {
        return if (value.isPure(true)) {
            { value.deepCopyWithSymbols() as IrExpressionWithCopy }
        } else {
            val varDeclaration = JsIrBuilder.buildVar(value.type, declaration, initializer = value)
            newStatements += varDeclaration
            { JsIrBuilder.buildGetValue(varDeclaration.symbol) }
        }
    }


    fun generateTypeCheck(argument: () -> IrExpressionWithCopy, toType: IrType): IrExpression {
        val toNotNullable = toType.makeNotNull()
        val argumentInstance = argument()

        val fromType = argumentInstance.type
        if (fromType.isInlinedWasm()) {
            // TODO: This is a dirty hack.
            if (fromType.classOrNull == toType.classOrNull) return JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
            val value = fromType.isSubtypeOf(toType, irBuiltIns = context.irBuiltIns)
            return JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, value)
        }

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

    abstract fun generateTypeCheckNonNull(argument: IrExpressionWithCopy, toType: IrType): IrExpression

    abstract fun lowerCoercionToUnit(expression: IrTypeOperatorCall): IrExpression

    abstract fun lowerIntegerCoercion(expression: IrTypeOperatorCall, declaration: IrDeclarationParent): IrExpression
}
