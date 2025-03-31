/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.serialization.sortDependencies
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.parentDeclarationsWithSelf
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.parents

object SortIrModules {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun sort(input: Iterable<IrModuleFragment>): List<IrModuleFragment> {
        val unique = input.toSet()
        if (unique.size <= 1) return unique.toList()

        val mapping1: Map<ModuleDescriptor, IrModuleFragment> = unique.associateBy { it.descriptor }
        val mapping2 = HashMap<IrModuleFragment, HashSet<IrModuleFragment>>()

        for (currentModule in input) {
            val referencedModules = HashSet<IrModuleFragment>().also { mapping2[currentModule] = it }

            currentModule.acceptVoid(object : IrTypeVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitDeclarationReference(expression: IrDeclarationReference) {
                    visitSymbol(expression.symbol)
                    super.visitDeclarationReference(expression)
                }

                override fun visitType(container: IrElement, type: IrType) {
                    visitSymbol((type as? IrSimpleType)?.classifier as? IrClassSymbol)
                }

                private fun visitSymbol(symbol: IrSymbol?) {
                    val referencedDeclaration = symbol?.owner as? IrDeclaration ?: return
                    val topLevelDeclaration = referencedDeclaration.parentDeclarationsWithSelf.last()

                    val referencedModule = topLevelDeclaration.fileOrNull?.module
                        ?: try {
                            mapping1.getValue(topLevelDeclaration.descriptor.parents.last() as ModuleDescriptor)
                        } catch (e: IllegalStateException) {
                            return
                        }

                    if (referencedModule != currentModule) {
                        referencedModules += referencedModule
                    }
                }
            })
        }

        return sortDependencies(mapping2)
    }
}