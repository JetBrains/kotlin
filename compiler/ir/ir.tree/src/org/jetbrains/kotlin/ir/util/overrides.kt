/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType

fun SymbolTable.declareSimpleFunctionWithOverrides(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: FunctionDescriptor
) =
    declareSimpleFunction(startOffset, endOffset, origin, descriptor).also { declaration ->
        generateOverriddenFunctionSymbols(declaration, this)
    }


fun generateOverriddenFunctionSymbols(
    declaration: IrSimpleFunction,
    symbolTable: SymbolTable
) {
    declaration.descriptor.overriddenDescriptors.mapTo(declaration.overriddenSymbols) {
        symbolTable.referenceSimpleFunction(it.original)
    }
}

fun SymbolTable.declareFieldWithOverrides(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: PropertyDescriptor,
    type: IrType,
    hasBackingField: (PropertyDescriptor) -> Boolean
) =
    declareField(startOffset, endOffset, origin, descriptor, type).also { declaration ->
        generateOverriddenFieldSymbols(declaration, this, hasBackingField)
    }

fun generateOverriddenFieldSymbols(
    declaration: IrField,
    symbolTable: SymbolTable,
    hasBackingField: (PropertyDescriptor) -> Boolean
) {
    declaration.descriptor.overriddenDescriptors.mapNotNullTo(declaration.overriddenSymbols) {
        if (hasBackingField(it)) {
            symbolTable.referenceField(it.original)
        } else null
    }
}