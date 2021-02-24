/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.multiplatform.findExpects

// Need to create unbound symbols for expects corresponding to actuals of the currently compiled module.
// This is necessary because there are no explicit links between expects and actuals
// neither in descriptors nor in IR.
internal fun referenceExpectsForUsedActuals(
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    symbolTable: SymbolTable,
    element: IrElement,
) {
    element.acceptVoid(ExpectDependencyGenerator(expectDescriptorToSymbol, symbolTable))
}

private class ExpectDependencyGenerator(
    private val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    private val symbolTable: SymbolTable,
) : IrElementVisitorVoid {
    private fun <T> T.forEachExpect(body: (DeclarationDescriptor) -> Unit) where T : IrDeclaration {
        this.descriptor.findExpects().forEach {
            body(it)
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        declaration.forEachExpect { expectDescriptor ->
            val symbol = symbolTable.referenceClass(expectDescriptor as ClassDescriptor)
            expectDescriptorToSymbol[expectDescriptor] = symbol
            expectDescriptor.constructors.forEach {
                expectDescriptorToSymbol[it] = symbolTable.referenceConstructor(it as ClassConstructorDescriptor)
            }
        }
        super.visitDeclaration(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.forEachExpect {
            val symbol = symbolTable.referenceSimpleFunction(it as FunctionDescriptor)
            expectDescriptorToSymbol[it] = symbol
        }
        super.visitDeclaration(declaration)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        declaration.forEachExpect {
            val symbol = symbolTable.referenceConstructor(it as ClassConstructorDescriptor)
            expectDescriptorToSymbol[it] = symbol

        }
        super.visitDeclaration(declaration)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.forEachExpect {
            val symbol = symbolTable.referenceProperty(it as PropertyDescriptor)
            expectDescriptorToSymbol[it] = symbol
        }
        super.visitDeclaration(declaration)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        declaration.forEachExpect {
            val symbol = symbolTable.referenceEnumEntry(it as ClassDescriptor)
            expectDescriptorToSymbol[it] = symbol

        }
        super.visitDeclaration(declaration)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        declaration.forEachExpect {
            val symbol = when (it) {
                is ClassDescriptor -> symbolTable.referenceClass(it)
                else -> error("Unexpected expect for actual type alias: $it")
            }
            expectDescriptorToSymbol[it] = symbol

        }
        super.visitDeclaration(declaration)
    }
}
