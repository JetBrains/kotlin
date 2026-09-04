/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class IrExpectWithDefaultDeclarationProvider : IrMissingActualDeclarationProvider() {
    override fun provideSymbolForMissingActual(
        expectSymbol: IrSymbol,
        containingExpectClassSymbol: IrClassSymbol?,
        containingActualClassSymbol: IrClassSymbol?
    ): IrSymbol? {
        if (expectSymbol.isExpectWithDefault) {
            expectSymbol.owner.accept(RemoveExpectTransformer(), null)
            return expectSymbol
        }
        return null
    }

    val IrSymbol.isExpectWithDefault: Boolean
        get() = when (val owner = owner) {
            is IrClass -> owner.isExpectWithDefault
            is IrFunction -> owner.isExpectWithDefault
            is IrProperty -> owner.isExpectWithDefault
            else -> false
        }

    class RemoveExpectTransformer : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            declaration.isExpect = false
            super.visitClass(declaration)
        }

        override fun visitFunction(declaration: IrFunction) {
            declaration.isExpect = false
            super.visitFunction(declaration)
        }

        override fun visitProperty(declaration: IrProperty) {
            declaration.isExpect = false
            super.visitProperty(declaration)
        }
    }
}
