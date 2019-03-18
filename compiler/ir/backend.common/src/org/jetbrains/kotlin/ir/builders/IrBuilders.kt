/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
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
    return IrVariableImpl(
        irExpression.startOffset,
        irExpression.endOffset,
        origin,
        symbol,
        Name.identifier(nameHint ?: "tmp"),
        varType,
        isMutable,
        false,
        false
    ).apply {
        initializer = irExpression
        parent = getLocalDeclarationParent()
        descriptor.bind(this)
    }
}