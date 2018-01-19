/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun <T : IrElement> T.patchOverriddenFunctionsFromDescriptors(symbolTable: SymbolTable) =
    apply {
        acceptVoid(PatchOverriddenFunctionsFromDescriptorsVisitor(symbolTable))
    }

class PatchOverriddenFunctionsFromDescriptorsVisitor(
    private val symbolTable: SymbolTable
) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.descriptor.overriddenDescriptors.mapTo(declaration.overriddenSymbols) {
            symbolTable.referenceSimpleFunction(it.original)
        }

        super.visitSimpleFunction(declaration)
    }
}
