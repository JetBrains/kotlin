/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

class DependenciesCollector {
    private val modulesForDependencyDescriptors = LinkedHashSet<ModuleDescriptor>()
    private val packageFragmentsForDependencyDescriptors = LinkedHashMap<ModuleDescriptor, MutableSet<PackageFragmentDescriptor>>()
    private val topLevelDescriptors = LinkedHashMap<PackageFragmentDescriptor, MutableSet<DeclarationDescriptor>>()

    val dependencyModules: Collection<ModuleDescriptor> get() = modulesForDependencyDescriptors

    fun getPackageFragments(moduleDescriptor: ModuleDescriptor): Collection<PackageFragmentDescriptor> =
        packageFragmentsForDependencyDescriptors[moduleDescriptor] ?: emptyList()

    fun getTopLevelDescriptors(packageFragmentDescriptor: PackageFragmentDescriptor): Collection<DeclarationDescriptor> =
        topLevelDescriptors[packageFragmentDescriptor] ?: emptyList()

    val isEmpty get() = topLevelDescriptors.isEmpty()

    fun collectTopLevelDescriptorsForUnboundSymbols(symbolTable: SymbolTable) {
        assert(symbolTable.unboundTypeParameters.isEmpty()) { "Unbound type parameters: ${symbolTable.unboundTypeParameters}" }
        assert(symbolTable.unboundValueParameters.isEmpty()) { "Unbound value parameters: ${symbolTable.unboundValueParameters}" }
        assert(symbolTable.unboundVariables.isEmpty()) { "Unbound variables: ${symbolTable.unboundVariables}" }

        symbolTable.markOverriddenFunctionsForUnboundFunctionsReferenced()
        symbolTable.markSuperClassesForUnboundClassesReferenced()

        symbolTable.unboundClasses.addTopLevelDeclarations()
        symbolTable.unboundConstructors.addTopLevelDeclarations()
        symbolTable.unboundEnumEntries.addTopLevelDeclarations()
        symbolTable.unboundFields.addTopLevelDeclarations()
        symbolTable.unboundSimpleFunctions.addTopLevelDeclarations()
    }

    private fun SymbolTable.markOverriddenFunctionsForUnboundFunctionsReferenced() {
        for (unboundFunction in unboundSimpleFunctions.toTypedArray()) {
            markOverriddenFunctionsReferenced(unboundFunction.descriptor, HashSet())
        }
    }

    private fun SymbolTable.markOverriddenFunctionsReferenced(
        function: FunctionDescriptor,
        visitedFunctions: MutableSet<FunctionDescriptor>
    ) {
        for (overridden in function.overriddenDescriptors) {
            if (overridden !in visitedFunctions) {
                visitedFunctions.add(overridden)
                referenceFunction(overridden.original)
                markOverriddenFunctionsReferenced(overridden, visitedFunctions)
            }
        }
    }

    private fun SymbolTable.markSuperClassesForUnboundClassesReferenced() {
        for (unboundClass in unboundClasses.toTypedArray()) {
            for (superClassifier in unboundClass.descriptor.getAllSuperClassifiers()) {
                if (superClassifier is ClassDescriptor) {
                    referenceClass(superClassifier)
                }
            }
        }
    }

    private fun Collection<IrSymbol>.addTopLevelDeclarations() {
        forEach {
            getTopLevelDeclaration(it.descriptor)?.let { addTopLevelDescriptor(it) }
        }
    }

    private fun DeclarationDescriptor.isSyntheticJavaProperties() =
        this is PropertyAccessorDescriptor &&
                this.kind == CallableMemberDescriptor.Kind.SYNTHESIZED &&
                containingDeclaration is ModuleDescriptor

    private fun getTopLevelDeclaration(descriptor: DeclarationDescriptor): DeclarationDescriptor? {
        val containingDeclaration = descriptor.containingDeclaration
        return when (containingDeclaration) {
            is PackageFragmentDescriptor -> descriptor
            is ClassDescriptor -> getTopLevelDeclaration(containingDeclaration)
            else ->
                if (descriptor.isDynamic() || descriptor.isSyntheticJavaProperties()) {
                    //skip
                    null
                } else {
                    throw AssertionError("Package or class expected: $containingDeclaration; for $descriptor")
                }
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
