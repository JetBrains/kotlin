package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.findActuals
import org.jetbrains.kotlin.resolve.multiplatform.findExpects

class ExpectActualTable(val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>) {
    val table = mutableMapOf<DeclarationDescriptor, IrSymbol>()

    private fun IrElement.recordActuals(rightHandSide: Map<DeclarationDescriptor, IrSymbol>, inModule: ModuleDescriptor) {
        this.acceptVoid(object : IrElementVisitorVoid {

            private fun recordDeclarationActuals(declaration: IrDeclaration) {

                expectDescriptorToSymbol.put(declaration.descriptor, (declaration as IrSymbolOwner).symbol)

                declaration.descriptor.findActuals(inModule).forEach {
                    val realActual = if (it is TypeAliasDescriptor)
                        it.expandedType.constructor.declarationDescriptor as? ClassDescriptor
                            ?: error("Unexpected actual typealias right hand side: $it")
                    else it

                    // TODO: what to do with fake overrides???
                    if (!(realActual is CallableMemberDescriptor && realActual.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)) {
                        table.put(
                            declaration.descriptor, rightHandSide[realActual]
                                ?: error("Could not find actual type alias target member for: ${declaration.descriptor} -> $it")
                        )
                    }
                }
            }

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                recordDeclarationActuals(declaration)
                super.visitFunction(declaration)
            }
            override fun visitClass(declaration: IrClass) {
                recordDeclarationActuals(declaration)
                super.visitClass(declaration)
            }
            override fun visitProperty(declaration: IrProperty) {
                recordDeclarationActuals(declaration)
                super.visitProperty(declaration)
            }
            override fun visitEnumEntry(declaration: IrEnumEntry) {
                recordDeclarationActuals(declaration)
                super.visitEnumEntry(declaration)
            }
        })
    }

    private fun IrDeclaration.recordRightHandSide(): Map<DeclarationDescriptor, IrSymbol> {
        val rightHandSide = hashMapOf<DeclarationDescriptor, IrSymbol>()

        this.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }
            override fun visitFunction(declaration: IrFunction) {
                rightHandSide.put(declaration.descriptor, declaration.symbol)
                super.visitFunction(declaration)
            }
            override fun visitClass(declaration: IrClass) {
                rightHandSide.put(declaration.descriptor, declaration.symbol)
                super.visitClass(declaration)
            }
            override fun visitProperty(declaration: IrProperty) {
                rightHandSide.put(declaration.descriptor, declaration.symbol)
                super.visitProperty(declaration)
            }
            override fun visitEnumEntry(declaration: IrEnumEntry) {
                rightHandSide.put(declaration.descriptor, declaration.symbol)
                super.visitEnumEntry(declaration)
            }
        })
        return rightHandSide
    }

    fun findExpectsForActuals(declaration: IrDeclaration) {
        if (declaration.descriptor !is MemberDescriptor) return

        val descriptor = declaration.symbol.descriptor

        if (declaration is IrTypeAlias && declaration.isActual) {
            val rightHandSide = declaration.expandedType.classOrNull?.owner?.recordRightHandSide()
                ?: error("Unexpected right hand side of actual typealias: ${declaration.descriptor}")


            declaration.descriptor.findExpects().forEach {
                expectDescriptorToSymbol[it]?.owner?.recordActuals(rightHandSide, declaration.descriptor.module)
            }
            return
        }

        val expects: List<MemberDescriptor> = if (descriptor is ClassConstructorDescriptor && descriptor.isPrimary) {
            descriptor.containingDeclaration.findExpects().mapNotNull {
                (it as ClassDescriptor).unsubstitutedPrimaryConstructor
            }
        } else {
            descriptor.findExpects()
        }

        expects.forEach { expect ->
            table.put(expect, declaration.symbol)
        }
    }
}
