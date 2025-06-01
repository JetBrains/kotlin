/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass

fun IrBuilderNew.irGetObjectValue(type: IrType, classSymbol: IrClassSymbol): IrGetObjectValue =
    IrGetObjectValueImpl(startOffset, endOffset, type, classSymbol)

fun IrBuilderNew.irGetObjectValue(type: IrType): IrGetObjectValue =
    irGetObjectValue(type, type.classOrFail)

fun IrBuilderNew.irGetObjectValue(classSymbol: IrClassSymbol): IrGetObjectValue =
    irGetObjectValue(IrSimpleTypeImpl(classSymbol, SimpleTypeNullability.DEFINITELY_NOT_NULL,  emptyList(), emptyList()), classSymbol)


fun IrBuilderNew.irGet(type: IrType, value: IrValueSymbol) : IrGetValue =
    IrGetValueImpl(startOffset, endOffset, type, value)

fun IrBuilderNew.irGet(value: IrValueDeclaration) = irGet(value.type, value.symbol)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irSet(variable: IrValueSymbol, value: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQ) =
    IrSetValueImpl(startOffset, endOffset, context.irBuiltIns.unitType, variable, value, origin)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irSet(variable: IrValueDeclaration, value: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQ) =
    irSet(variable.symbol, value, origin)

fun IrBuilderNew.irGetField(receiver: IrExpression?, field: IrFieldSymbol, type: IrType): IrGetField =
    IrGetFieldImpl(startOffset, endOffset, field, type, receiver)

fun IrBuilderNew.irGetField(receiver: IrExpression?, field: IrField): IrGetField =
    irGetField(receiver, field.symbol, field.type)


context(context: IrBuiltInsAware)
fun IrBuilderNew.irSetField(receiver: IrExpression?, field: IrFieldSymbol, value: IrExpression, origin: IrStatementOrigin? = null): IrSetField =
    IrSetFieldImpl(startOffset, endOffset, field, receiver, value, context.irBuiltIns.unitType, origin = origin)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irSetField(receiver: IrExpression?, field: IrField, value: IrExpression, origin: IrStatementOrigin? = null): IrSetField =
    irSetField(receiver, field.symbol, value, origin)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irInstanceInitializer(classSymbol: IrClassSymbol): IrExpression =
    IrInstanceInitializerCallImpl(
        startOffset, endOffset,
        classSymbol,
        context.irBuiltIns.unitType
    )

context(context: IrBuiltInsAware, parent: DeclarationParentScope)
fun IrBuilderNew.irInstanceInitializer(): IrExpression =
    irInstanceInitializer((parent.parent as IrConstructor).constructedClass.symbol)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irConcat(arguments: List<IrExpression>): IrStringConcatenation =
    IrStringConcatenationImpl(startOffset, endOffset, context.irBuiltIns.stringType, arguments)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irVararg(elementType: IrType, values: List<IrVarargElement>): IrVararg =
    IrVarargImpl(startOffset, endOffset, context.irBuiltIns.arrayClass.typeWith(elementType), elementType, values)

fun IrBuilderNew.irRawFunctionReference(type: IrType, symbol: IrFunctionSymbol): IrRawFunctionReferenceImpl =
    IrRawFunctionReferenceImpl(startOffset, endOffset, type, symbol)


fun IrBuilderNew.irRichFunctionReference(
    invokeFunction: IrSimpleFunction,
    superType: IrType,
    reflectionTargetSymbol: IrFunctionSymbol?,
    overriddenFunctionSymbol: IrSimpleFunctionSymbol,
    captures: List<IrExpression>,
    origin: IrStatementOrigin?,
): IrRichFunctionReference = IrRichFunctionReferenceImpl(
    startOffset = startOffset,
    endOffset = endOffset,
    type = superType,
    reflectionTargetSymbol = reflectionTargetSymbol,
    overriddenFunctionSymbol = overriddenFunctionSymbol,
    invokeFunction = invokeFunction,
    origin = origin
).apply {
    boundValues += captures
}
