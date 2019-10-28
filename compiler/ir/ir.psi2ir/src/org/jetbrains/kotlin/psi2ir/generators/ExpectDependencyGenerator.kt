package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.multiplatform.findExpects

// Need to create unbound symbols for expects corresponding to actuals of the currently compiled module.
// This is neccessary because there is no explicit links between expects and actuals
// neither in descriptors nor in IR.
fun referenceExpectsForUsedActuals(expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>, symbolTable: SymbolTable, irModule: IrModuleFragment) {
    irModule.acceptVoid(object : IrElementVisitorVoid {

        private fun <T> T.forEachExpect(body: (DeclarationDescriptor) -> Unit) where T: IrDeclaration {
            this.descriptor.findExpects().forEach {
                body(it)
            }
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }
        override fun visitClass(declaration: IrClass) {
            declaration.forEachExpect {
                val symbol = symbolTable.referenceClass(it as ClassDescriptor)
                expectDescriptorToSymbol.put(it, symbol);
                it.constructors.forEach {
                    expectDescriptorToSymbol.put(it, symbolTable.referenceConstructor(it as ClassConstructorDescriptor))
                }
            }
            super.visitDeclaration(declaration)
        }
        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            declaration.forEachExpect {
                val symbol = symbolTable.referenceSimpleFunction(it as FunctionDescriptor);
                expectDescriptorToSymbol.put(it, symbol);
            }
            super.visitDeclaration(declaration)
        }
        override fun visitConstructor(declaration: IrConstructor) {
            declaration.forEachExpect {
                val symbol = symbolTable.referenceConstructor(it as ClassConstructorDescriptor)
                expectDescriptorToSymbol.put(it, symbol);
            
            }
            super.visitDeclaration(declaration)
        }
        override fun visitProperty(declaration: IrProperty) {
            declaration.forEachExpect {
                val symbol = symbolTable.referenceProperty(it as PropertyDescriptor)
                expectDescriptorToSymbol.put(it, symbol);
            }
            super.visitDeclaration(declaration)
        }
        override fun visitEnumEntry(declaration: IrEnumEntry) {
            declaration.forEachExpect {
                val symbol = symbolTable.referenceEnumEntry(it as ClassDescriptor)
                expectDescriptorToSymbol.put(it, symbol);

            }
            super.visitDeclaration(declaration)
        }
        override fun visitTypeAlias(declaration: IrTypeAlias) {
            // Force actual type alias right hand side deserialization.
            if (declaration.isActual) {
                declaration.expandedType.classOrNull?.descriptor?.let { symbolTable.referenceClass(it) }
            }

            declaration.forEachExpect {
                val symbol = when (it) {
                    is ClassDescriptor -> symbolTable.referenceClass(it)
                    else -> error("Unexpected expect for actual type alias: $it")
                }
                expectDescriptorToSymbol.put(it, symbol);

            }
            super.visitDeclaration(declaration)
        }
    })
}
