/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.types.KotlinType

object JsIrBuilder {

    object SYNTHESIZED_STATEMENT : IrStatementOriginImpl("SYNTHESIZED_STATEMENT")
    object SYNTHESIZED_DECLARATION : IrDeclarationOriginImpl("SYNTHESIZED_DECLARATION")

    fun buildCall(target: IrFunctionSymbol, type: KotlinType? = null, typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null) =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type ?: target.descriptor.returnType!!,
            target,
            target.descriptor,
            typeArguments,
            SYNTHESIZED_STATEMENT
        )

    fun buildReturn(targetSymbol: IrFunctionSymbol, value: IrExpression) =
        IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, targetSymbol, value)

    fun buildValueParameter(symbol: IrValueParameterSymbol) =
        IrValueParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol)

    fun buildFunction(symbol: IrSimpleFunctionSymbol) = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol)

    fun buildGetObjectValue(type: KotlinType, classSymbol: IrClassSymbol) =
        IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, classSymbol)
    fun buildGetClass(expression: IrExpression, type: KotlinType) = IrGetClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, expression)

    fun buildGetValue(symbol: IrValueSymbol) = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, SYNTHESIZED_STATEMENT)
    fun buildSetVariable(symbol: IrVariableSymbol, value: IrExpression) =
        IrSetVariableImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, value, SYNTHESIZED_STATEMENT)

    fun buildGetField(symbol: IrFieldSymbol, receiver: IrExpression?, superQualifierSymbol: IrClassSymbol? = null) =
        IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, receiver, SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildSetField(symbol: IrFieldSymbol, receiver: IrExpression?, value: IrExpression, superQualifierSymbol: IrClassSymbol? = null) =
        IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, receiver, value, SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildBlockBody(statements: List<IrStatement>) = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)

    fun buildBlock(type: KotlinType) = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT)
    fun buildBlock(type: KotlinType, statements: List<IrStatement>) =
        IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, statements)

    fun buildFunctionReference(type: KotlinType, symbol: IrFunctionSymbol) =
        IrFunctionReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, symbol.descriptor)

    fun buildVar(symbol: IrVariableSymbol, initializer: IrExpression? = null) =
        IrVariableImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol).apply { this.initializer = initializer }

    fun buildBreak(type: KotlinType, loop: IrLoop) = IrBreakImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)
    fun buildContinue(type: KotlinType, loop: IrLoop) = IrContinueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)

    fun buildIfElse(type: KotlinType, cond: IrExpression, thenBranch: IrExpression, elseBranch: IrExpression? = null) = IrIfThenElseImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, cond, thenBranch, elseBranch, SYNTHESIZED_STATEMENT
    )

    fun buildNull(type: KotlinType) = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)
    fun buildBoolean(type: KotlinType, v: Boolean) = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, v)
}