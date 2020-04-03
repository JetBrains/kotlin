/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

object JsIrBuilder {

    object SYNTHESIZED_STATEMENT : IrStatementOriginImpl("SYNTHESIZED_STATEMENT")
    object SYNTHESIZED_DECLARATION : IrDeclarationOriginImpl("SYNTHESIZED_DECLARATION")

    fun buildCall(target: IrFunctionSymbol, type: IrType? = null, typeArguments: List<IrType>? = null): IrCall =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type ?: target.owner.returnType,
            target,
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

    fun buildValueParameter(name: String = "tmp", index: Int, type: IrType) = buildValueParameter(Name.identifier(name), index, type)

    fun buildValueParameter(name: Name, index: Int, type: IrType, origin: IrDeclarationOrigin = SYNTHESIZED_DECLARATION): IrValueParameter {
        val descriptor = WrappedValueParameterDescriptor()
        return IrValueParameterImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin,
            IrValueParameterSymbolImpl(descriptor),
            name,
            index,
            type,
            null,
            isCrossinline = false,
            isNoinline = false
        ).also {
            descriptor.bind(it)
        }
    }

    fun buildTypeParameter(name: Name, index: Int, isReified: Boolean, variance: Variance = Variance.INVARIANT): IrTypeParameter {
        val descriptor = WrappedTypeParameterDescriptor()
        return IrTypeParameterImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            SYNTHESIZED_DECLARATION,
            IrTypeParameterSymbolImpl(descriptor),
            name,
            index,
            isReified,
            variance
        ).also {
            descriptor.bind(it)
        }
    }

    fun buildFunction(
        name: String,
        returnType: IrType,
        parent: IrDeclarationParent,
        visibility: Visibility = Visibilities.PUBLIC,
        modality: Modality = Modality.FINAL,
        isInline: Boolean = false,
        isExternal: Boolean = false,
        isTailrec: Boolean = false,
        isSuspend: Boolean = false,
        isExpect: Boolean = false,
        isFakeOverride: Boolean = false,
        origin: IrDeclarationOrigin = SYNTHESIZED_DECLARATION
    ) = buildFunction(
        Name.identifier(name),
        returnType,
        parent,
        visibility,
        modality,
        isInline = isInline,
        isExternal = isExternal,
        isTailrec = isTailrec,
        isSuspend = isSuspend,
        isExpect = isExpect,
        isFakeOverride = isFakeOverride,
        origin = origin
    )

    fun buildFunction(
        name: Name,
        returnType: IrType,
        parent: IrDeclarationParent,
        visibility: Visibility = Visibilities.PUBLIC,
        modality: Modality = Modality.FINAL,
        isInline: Boolean = false,
        isExternal: Boolean = false,
        isTailrec: Boolean = false,
        isSuspend: Boolean = false,
        isExpect: Boolean = false,
        isOperator: Boolean = false,
        isFakeOverride: Boolean = false,
        origin: IrDeclarationOrigin = SYNTHESIZED_DECLARATION
    ): IrSimpleFunction {
        val descriptor = WrappedSimpleFunctionDescriptor()
        return IrFunctionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin,
            IrSimpleFunctionSymbolImpl(descriptor),
            name,
            visibility,
            modality,
            returnType,
            isInline = isInline,
            isExternal = isExternal,
            isTailrec = isTailrec,
            isSuspend = isSuspend,
            isExpect = isExpect,
            isFakeOverride = isFakeOverride,
            isOperator = isOperator
        ).also {
            descriptor.bind(it)
            it.parent = parent
        }
    }

    fun buildAnonymousInitializer() =
        IrAnonymousInitializerImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, IrAnonymousInitializerSymbolImpl(
            WrappedClassDescriptor()
        ))

    fun buildGetObjectValue(type: IrType, classSymbol: IrClassSymbol) =
        IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, classSymbol)

    fun buildGetValue(symbol: IrValueSymbol) =
        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.type, symbol, SYNTHESIZED_STATEMENT)

    fun buildSetVariable(symbol: IrVariableSymbol, value: IrExpression, type: IrType) =
        IrSetVariableImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, value, SYNTHESIZED_STATEMENT)

    fun buildGetField(symbol: IrFieldSymbol, receiver: IrExpression?, superQualifierSymbol: IrClassSymbol? = null, type: IrType? = null) =
        IrGetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            type ?: symbol.owner.type,
            receiver,
            SYNTHESIZED_STATEMENT,
            superQualifierSymbol
        )

    fun buildSetField(
        symbol: IrFieldSymbol,
        receiver: IrExpression?,
        value: IrExpression,
        type: IrType,
        superQualifierSymbol: IrClassSymbol? = null
    ) =
        IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, receiver, value, type, SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildBlockBody(statements: List<IrStatement>) = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)

    fun buildBlock(type: IrType) = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT)
    fun buildBlock(type: IrType, statements: List<IrStatement>) =
        IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, statements)

    fun buildComposite(type: IrType, statements: List<IrStatement> = emptyList()) =
        IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, statements)

    fun buildFunctionReference(type: IrType, symbol: IrFunctionSymbol, reflectionTarget: IrFunctionSymbol? = symbol) =
        IrFunctionReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, 0, reflectionTarget, null)

    fun buildFunctionExpression(type: IrType, function: IrSimpleFunction) =
        IrFunctionExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, function, SYNTHESIZED_STATEMENT)

    fun buildVar(
        type: IrType,
        parent: IrDeclarationParent?,
        name: String = "tmp",
        isVar: Boolean = false,
        isConst: Boolean = false,
        isLateinit: Boolean = false,
        initializer: IrExpression? = null
    ): IrVariable {
        val descriptor = WrappedVariableDescriptor()
        return IrVariableImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            SYNTHESIZED_DECLARATION,
            IrVariableSymbolImpl(descriptor),
            Name.identifier(name),
            type,
            isVar,
            isConst,
            isLateinit
        ).also {
            descriptor.bind(it)
            it.initializer = initializer
            if (parent != null) it.parent = parent
        }
    }

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

    fun buildWhen(type: IrType, branches: List<IrBranch>, origin: IrStatementOrigin = SYNTHESIZED_STATEMENT) =
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