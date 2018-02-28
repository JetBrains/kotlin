/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

fun SymbolTable.declareSimpleFunctionWithOverrides(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: FunctionDescriptor
) =
    declareSimpleFunction(startOffset, endOffset, origin, descriptor).also { declaration ->
        generateOverriddenSymbols(declaration, this)
    }

fun generateOverriddenSymbols(
    declaration: IrSimpleFunction,
    symbolTable: SymbolTable
) {
    declaration.descriptor.overriddenDescriptors.mapTo(declaration.overriddenSymbols) {
        symbolTable.referenceSimpleFunction(it.original)
    }
}
