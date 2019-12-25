/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBreakImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrContinueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.name.Name

fun IrBuilderWithScope.irWhile(origin: IrStatementOrigin? = null) =
    IrWhileLoopImpl(startOffset, endOffset, context.irBuiltIns.unitType, origin)

fun IrBuilderWithScope.irBreak(loop: IrLoop) =
    IrBreakImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)

fun IrBuilderWithScope.irContinue(loop: IrLoop) =
    IrContinueImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)

fun IrBuilderWithScope.irGetObject(classSymbol: IrClassSymbol) =
    IrGetObjectValueImpl(startOffset, endOffset, IrSimpleTypeImpl(classSymbol, false, emptyList(), emptyList()), classSymbol)

// Also adds created variable into building block
fun <T : IrElement> IrStatementsBuilder<T>.createTmpVariable(
    irExpression: IrExpression,
    nameHint: String? = null,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    irType: IrType? = null
): IrVariable {
    val variable = scope.createTmpVariable(irExpression, nameHint, isMutable, origin, irType)
    +variable
    return variable
}

fun Scope.createTmpVariable(
    irExpression: IrExpression,
    nameHint: String? = null,
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
    irType: IrType? = null
): IrVariable {
    val varType = irType ?: irExpression.type
    val descriptor = WrappedVariableDescriptor()
    val symbol = IrVariableSymbolImpl(descriptor)
    return irDeclarationFactory.createVariable(
        irExpression.startOffset,
        irExpression.endOffset,
        origin,
        symbol,
        Name.identifier(nameHint ?: "tmp"),
        varType,
        isMutable,
        isConst = false,
        isLateinit = false
    ).apply {
        initializer = irExpression
        parent = getLocalDeclarationParent()
        descriptor.bind(this)
    }
}