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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.resolve.DescriptorUtils

class DependenciesCollector {
    private val modulesForDependencyDescriptors = LinkedHashSet<ModuleDescriptor>()
    private val packageFragmentsForDependencyDescriptors = LinkedHashMap<ModuleDescriptor, MutableSet<PackageFragmentDescriptor>>()
    private val topLevelDescriptors = LinkedHashMap<PackageFragmentDescriptor, MutableSet<DeclarationDescriptor>>()

    val dependencyModules: Collection<ModuleDescriptor> get() = modulesForDependencyDescriptors

    fun getPackageFragments(moduleDescriptor: ModuleDescriptor): Collection<PackageFragmentDescriptor> =
            packageFragmentsForDependencyDescriptors[moduleDescriptor] ?: emptyList()

    fun getTopLevelDescriptors(packageFragmentDescriptor: PackageFragmentDescriptor): Collection<DeclarationDescriptor> =
            topLevelDescriptors[packageFragmentDescriptor] ?: emptyList()

    fun collectTopLevelDescriptorsForUnboundSymbols(symbolTable: SymbolTable) {
        assert(symbolTable.unboundTypeParameters.isEmpty()) { "Unbound type parameters: ${symbolTable.unboundTypeParameters}" }
        assert(symbolTable.unboundValueParameters.isEmpty()) { "Unbound value parameters: ${symbolTable.unboundValueParameters}" }
        assert(symbolTable.unboundVariables.isEmpty()) { "Unbound variables: ${symbolTable.unboundVariables}" }

        symbolTable.unboundClasses.addTopLevelDeclarations()
        symbolTable.unboundConstructors.addTopLevelDeclarations()
        symbolTable.unboundEnumEntries.addTopLevelDeclarations()
        symbolTable.unboundFields.addTopLevelDeclarations()
        symbolTable.unboundSimpleFunctions.addTopLevelDeclarations()
    }

    private fun Collection<IrSymbol>.addTopLevelDeclarations() {
        forEach { addTopLevelDeclaration(it) }
    }

    fun addTopLevelDeclaration(symbol: IrSymbol) {
        val descriptor = symbol.descriptor
        val topLevelDeclaration = getTopLevelDeclaration(descriptor)
        addTopLevelDescriptor(topLevelDeclaration)
    }

    private fun getTopLevelDeclaration(descriptor: DeclarationDescriptor): DeclarationDescriptor {
        val containingDeclaration = descriptor.containingDeclaration
        return when (containingDeclaration) {
            is PackageFragmentDescriptor -> descriptor
            is ClassDescriptor -> getTopLevelDeclaration(containingDeclaration)
            else -> throw AssertionError("Package or class expected: $containingDeclaration")
        }
    }

    private fun addTopLevelDescriptor(descriptor: DeclarationDescriptor) {
        val packageFragmentDescriptor = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java)!!

        val moduleDescriptor = packageFragmentDescriptor.containingDeclaration
        modulesForDependencyDescriptors.add(moduleDescriptor)

        packageFragmentsForDependencyDescriptors.getOrPut(moduleDescriptor) { LinkedHashSet() }.add(packageFragmentDescriptor)
        topLevelDescriptors.getOrPut(packageFragmentDescriptor) { LinkedHashSet() }.add(descriptor)
    }
}
