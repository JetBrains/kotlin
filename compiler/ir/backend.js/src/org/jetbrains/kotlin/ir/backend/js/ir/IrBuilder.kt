/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.ir.backend.web.JsStatementOrigins
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name

object JsIrBuilder {
    object SYNTHESIZED_DECLARATION : IrDeclarationOriginImpl("SYNTHESIZED_DECLARATION")

    fun buildCall(
        target: IrSimpleFunctionSymbol,
        type: IrType? = null,
        typeArguments: List<IrType>? = null,
        origin: IrStatementOrigin = JsStatementOrigins.SYNTHESIZED_STATEMENT,
        superQualifierSymbol: IrClassSymbol? = null,
    ): IrCall {
        val owner = target.owner
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type ?: owner.returnType,
            target,
            superQualifierSymbol = superQualifierSymbol,
            typeArgumentsCount = owner.typeParameters.size,
            valueArgumentsCount = owner.valueParameters.size,
            origin = origin
        ).apply {
            typeArguments?.let {
                assert(typeArguments.size == typeArgumentsCount)
                it.withIndex().forEach { (i, t) -> putTypeArgument(i, t) }
            }
        }
    }

    fun buildArray(elements: List<IrExpression>, type: IrType, elementType: IrType): IrExpression {
        return IrVarargImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            elementType,
            elements
        )
    }

    fun buildDelegatingConstructorCall(target: IrConstructorSymbol, typeArguments: List<IrType?>? = null): IrDelegatingConstructorCall {
        val owner = target.owner
        val irClass = owner.parentAsClass

        return IrDelegatingConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            owner.returnType,
            target,
            typeArgumentsCount = irClass.typeParameters.size,
            valueArgumentsCount = owner.valueParameters.size,
        ).apply {
            typeArguments?.let {
                assert(it.size == typeArgumentsCount)
                it.withIndex().forEach { (i, t) -> putTypeArgument(i, t) }
            }
        }
    }

    fun buildConstructorCall(
        target: IrConstructorSymbol,
        typeArguments: List<IrType?>? = null,
        constructorTypeArguments: List<IrType?>? = null,
        origin: IrStatementOrigin = JsStatementOrigins.SYNTHESIZED_STATEMENT
    ): IrConstructorCall {
        val owner = target.owner
        val irClass = owner.parentAsClass

        return IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            owner.returnType,
            target,
            typeArgumentsCount = irClass.typeParameters.size,
            constructorTypeArgumentsCount = owner.typeParameters.size,
            valueArgumentsCount = owner.valueParameters.size,
            origin = origin
        ).apply {
            typeArguments?.let {
                assert(it.size == typeArgumentsCount)
                it.withIndex().forEach { (i, t) -> putTypeArgument(i, t) }
            }

            constructorTypeArguments?.let {
                assert(it.size == typeArgumentsCount)
                it.withIndex().forEach { (i, t) -> putTypeArgument(i, t) }
            }
        }
    }

    fun buildRawReference(targetSymbol: IrFunctionSymbol, type: IrType): IrRawFunctionReference =
        IrRawFunctionReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, targetSymbol)

    fun buildReturn(targetSymbol: IrFunctionSymbol, value: IrExpression, type: IrType) =
        IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, targetSymbol, value)

    fun buildThrow(type: IrType, value: IrExpression) = IrThrowImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, value)

    fun buildValueParameter(
        parent: IrFunction,
        name: String,
        index: Int,
        type: IrType,
        isAssignable: Boolean = false,
        origin: IrDeclarationOrigin = SYNTHESIZED_DECLARATION
    ): IrValueParameter =
        buildValueParameter(parent) {
            this.origin = origin
            this.name = Name.identifier(name)
            this.index = index
            this.type = type
            this.isAssignable = isAssignable
        }

    fun buildGetObjectValue(type: IrType, classSymbol: IrClassSymbol) =
        IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, classSymbol)

    fun buildGetValue(symbol: IrValueSymbol, origin: IrStatementOrigin = JsStatementOrigins.SYNTHESIZED_STATEMENT) =
        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.type, symbol, origin)

    fun buildSetValue(symbol: IrValueSymbol, value: IrExpression) =
        IrSetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.type, symbol, value, JsStatementOrigins.SYNTHESIZED_STATEMENT)

    fun buildSetVariable(symbol: IrVariableSymbol, value: IrExpression, type: IrType) =
        IrSetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, value, JsStatementOrigins.SYNTHESIZED_STATEMENT)

    fun buildGetField(symbol: IrFieldSymbol, receiver: IrExpression?, superQualifierSymbol: IrClassSymbol? = null, type: IrType? = null) =
        IrGetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            type ?: symbol.owner.type,
            receiver,
            JsStatementOrigins.SYNTHESIZED_STATEMENT,
            superQualifierSymbol
        )

    fun buildSetField(
        symbol: IrFieldSymbol,
        receiver: IrExpression?,
        value: IrExpression,
        type: IrType,
        superQualifierSymbol: IrClassSymbol? = null
    ) =
        IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, receiver, value, type,
                       JsStatementOrigins.SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildBlock(type: IrType) = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, JsStatementOrigins.SYNTHESIZED_STATEMENT)
    fun buildBlock(type: IrType, statements: List<IrStatement>) =
        IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, JsStatementOrigins.SYNTHESIZED_STATEMENT, statements)

    fun buildComposite(type: IrType, statements: List<IrStatement> = emptyList()) =
        IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, JsStatementOrigins.SYNTHESIZED_STATEMENT, statements)

    fun buildFunctionExpression(type: IrType, function: IrSimpleFunction) =
        IrFunctionExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, function, JsStatementOrigins.SYNTHESIZED_STATEMENT)

    fun buildVar(
        type: IrType,
        parent: IrDeclarationParent?,
        name: String = "tmp",
        isVar: Boolean = false,
        isConst: Boolean = false,
        isLateinit: Boolean = false,
        initializer: IrExpression? = null
    ): IrVariable = buildVariable(
        parent,
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        SYNTHESIZED_DECLARATION,
        Name.identifier(name),
        type,
        isVar,
        isConst,
        isLateinit,
    ).also {
        it.initializer = initializer
    }

    fun buildBreak(type: IrType, loop: IrLoop) = IrBreakImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)
    fun buildContinue(type: IrType, loop: IrLoop) = IrContinueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)

    fun buildIfElse(
        type: IrType,
        cond: IrExpression,
        thenBranch: IrExpression,
        elseBranch: IrExpression? = null,
        thenBranchStartOffset: Int = cond.startOffset,
        thenBranchEndOffset: Int = thenBranch.endOffset,
        elseBranchStartOffset: Int = UNDEFINED_OFFSET,
        elseBranchEndOffset: Int = elseBranch?.endOffset ?: UNDEFINED_OFFSET,
    ): IrWhen =
        buildIfElse(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = type,
            cond = cond,
            thenBranch = thenBranch,
            elseBranch = elseBranch,
            origin = JsStatementOrigins.SYNTHESIZED_STATEMENT,
            thenBranchStartOffset = thenBranchStartOffset,
            thenBranchEndOffset = thenBranchEndOffset,
            elseBranchStartOffset = elseBranchStartOffset,
            elseBranchEndOffset = elseBranchEndOffset
        )

    fun buildIfElse(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        cond: IrExpression,
        thenBranch: IrExpression,
        elseBranch: IrExpression? = null,
        origin: IrStatementOrigin? = null,
        thenBranchStartOffset: Int = cond.startOffset,
        thenBranchEndOffset: Int = thenBranch.endOffset,
        elseBranchStartOffset: Int = UNDEFINED_OFFSET,
        elseBranchEndOffset: Int = elseBranch?.endOffset ?: UNDEFINED_OFFSET,
    ): IrWhen {
        val element = IrIfThenElseImpl(startOffset, endOffset, type, origin)
        element.branches.add(IrBranchImpl(thenBranchStartOffset, thenBranchEndOffset, cond, thenBranch))
        if (elseBranch != null) {
            val irTrue = IrConstImpl.constTrue(UNDEFINED_OFFSET, UNDEFINED_OFFSET, cond.type)
            element.branches.add(IrElseBranchImpl(elseBranchStartOffset, elseBranchEndOffset, irTrue, elseBranch))
        }

        return element
    }

    fun buildWhen(type: IrType, branches: List<IrBranch>, origin: IrStatementOrigin = JsStatementOrigins.SYNTHESIZED_STATEMENT) =
        IrWhenImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, origin, branches)

    fun buildTypeOperator(type: IrType, operator: IrTypeOperator, argument: IrExpression, toType: IrType) =
        IrTypeOperatorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, operator, toType, argument)

    fun buildImplicitCast(value: IrExpression, toType: IrType) =
        buildTypeOperator(toType, IrTypeOperator.IMPLICIT_CAST, value, toType)

    fun buildReinterpretCast(value: IrExpression, toType: IrType) =
        buildTypeOperator(toType, IrTypeOperator.REINTERPRET_CAST, value, toType)

    fun buildNull(type: IrType) = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)
    fun buildBoolean(type: IrType, v: Boolean) = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, v)
    fun buildInt(type: IrType, v: Int) = IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, v)
    fun buildString(type: IrType, s: String) = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, s)
    fun buildTry(type: IrType) = IrTryImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)
    fun buildCatch(ex: IrVariable, block: IrBlockImpl) = IrCatchImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, ex, block)
}
