/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType

object JsIrBuilder {

    object SYNTHESIZED_STATEMENT : IrStatementOriginImpl("SYNTHESIZED_STATEMENT")
    object SYNTHESIZED_DECLARATION : IrDeclarationOriginImpl("SYNTHESIZED_DECLARATION")

    fun buildCall(target: IrFunctionSymbol, type: IrType? = null, typeArguments: List<IrType>? = null): IrCall =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type ?: target.owner.returnType,
            target,
            target.descriptor,
            target.descriptor.typeParametersCount,
            SYNTHESIZED_STATEMENT
        ).apply {
            typeArguments?.let {
                assert(typeArguments.size == typeArgumentsCount)
                it.withIndex().forEach { (i, t) -> putTypeArgument(i, t) }
            }
        }

    fun buildReturn(targetSymbol: IrFunctionSymbol, value: IrExpression, type: IrType) =
        IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, targetSymbol, value)

    fun buildThrow(type: IrType, value: IrExpression) = IrThrowImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, value)

    fun buildValueParameter(symbol: IrValueParameterSymbol, type: IrType? = null) =
        IrValueParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol, type ?: symbol.owner.type, null)

    fun buildFunction(symbol: IrSimpleFunctionSymbol, returnType: IrType) =
        IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol).apply {
            this.returnType = returnType
        }

    fun buildGetObjectValue(type: IrType, classSymbol: IrClassSymbol) =
        IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, classSymbol)

    fun buildGetClass(expression: IrExpression, type: IrType) = IrGetClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, expression)

    fun buildGetValue(symbol: IrValueSymbol) = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.type, symbol, SYNTHESIZED_STATEMENT)
    fun buildSetVariable(symbol: IrVariableSymbol, value: IrExpression, type: IrType) =
        IrSetVariableImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, value, SYNTHESIZED_STATEMENT)

    fun buildGetField(symbol: IrFieldSymbol, receiver: IrExpression?, superQualifierSymbol: IrClassSymbol? = null, type: IrType? = null) =
        IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, type ?: symbol.owner.type, receiver, SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildSetField(symbol: IrFieldSymbol, receiver: IrExpression?, value: IrExpression, type: IrType, superQualifierSymbol: IrClassSymbol? = null) =
        IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, receiver, value, type, SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildBlockBody(statements: List<IrStatement>) = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)

    fun buildBlock(type: IrType) = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT)
    fun buildBlock(type: IrType, statements: List<IrStatement>) =
        IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, statements)

    fun buildComposite(type: IrType, statements: List<IrStatement> = emptyList()) =
        IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, statements)

    fun buildFunctionReference(type: IrType, symbol: IrFunctionSymbol) =
        IrFunctionReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, symbol.descriptor, 0, null)

    fun buildVar(symbol: IrVariableSymbol, initializer: IrExpression? = null, type: IrType? = null) =
        IrVariableImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol, type ?: symbol.owner.type)
            .apply { this.initializer = initializer }

    fun buildBreak(type: IrType, loop: IrLoop) = IrBreakImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)
    fun buildContinue(type: IrType, loop: IrLoop) = IrContinueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)

    fun buildIfElse(type: IrType, cond: IrExpression, thenBranch: IrExpression, elseBranch: IrExpression? = null): IrWhen = buildIfElse(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, cond, thenBranch, elseBranch, SYNTHESIZED_STATEMENT
    )

    fun buildIfElse(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        cond: IrExpression,
        thenBranch: IrExpression,
        elseBranch: IrExpression? = null,
        origin: IrStatementOrigin? = null
    ): IrWhen {
        val element = IrIfThenElseImpl(startOffset, endOffset, type, origin)
        element.branches.add(IrBranchImpl(cond, thenBranch))
        if (elseBranch != null) {
            val irTrue = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, cond.type, true)
            element.branches.add(IrElseBranchImpl(irTrue, elseBranch))
        }

        return element
    }

    fun buildWhen(type: IrType, branches: List<IrBranch>) =
        IrWhenImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, branches)

    fun buildTypeOperator(type: IrType, operator: IrTypeOperator, argument: IrExpression, toType: IrType, symbol: IrClassifierSymbol) =
        IrTypeOperatorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, operator, toType, symbol, argument)

    fun buildNull(type: IrType) = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)
    fun buildBoolean(type: IrType, v: Boolean) = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, v)
    fun buildInt(type: IrType, v: Int) = IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, v)
    fun buildString(type: IrType, s: String) = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, s)
}
