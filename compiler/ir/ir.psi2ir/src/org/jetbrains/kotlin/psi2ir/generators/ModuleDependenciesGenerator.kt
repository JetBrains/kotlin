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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi2ir.StableDescriptorsComparator
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class ModuleDependenciesGenerator(override val context: GeneratorContext) : Generator {
    private class DependenciesCollector {
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

    fun generateUnboundSymbolsAsDependencies(irModule: IrModuleFragment) {
        val collector = DependenciesCollector()
        collector.collectTopLevelDescriptorsForUnboundSymbols(context.symbolTable)

        collector.dependencyModules.mapTo(irModule.dependencyModules) { moduleDescriptor ->
            generateModuleStub(collector, moduleDescriptor)
        }
    }

    private fun generateModuleStub(collector: DependenciesCollector, moduleDescriptor: ModuleDescriptor): IrModuleFragmentImpl =
            IrModuleFragmentImpl(moduleDescriptor, context.irBuiltIns).also { irDependencyModule ->
                collector.getPackageFragments(moduleDescriptor).mapTo(irDependencyModule.externalPackageFragments) { packageFragmentDescriptor ->
                    generatePackageStub(packageFragmentDescriptor, collector.getTopLevelDescriptors(packageFragmentDescriptor))
                }
            }

    private fun generatePackageStub(packageFragmentDescriptor: PackageFragmentDescriptor, topLevelDescriptors: Collection<DeclarationDescriptor>): IrExternalPackageFragment =
            context.symbolTable.declareExternalPackageFragment(packageFragmentDescriptor).also { irExternalPackageFragment ->
                topLevelDescriptors.mapTo(irExternalPackageFragment.declarations) {
                    generateStub(it)
                }
            }

    private fun generateStub(descriptor: DeclarationDescriptor): IrDeclaration =
            when (descriptor) {
                is ClassDescriptor ->
                    if (DescriptorUtils.isEnumEntry(descriptor))
                        generateEnumEntryStub(descriptor)
                    else
                        generateClassStub(descriptor)
                is ClassConstructorDescriptor ->
                    generateConstructorStub(descriptor)
                is FunctionDescriptor ->
                    generateFunctionStub(descriptor)
                is PropertyDescriptor ->
                    generatePropertyStub(descriptor)
                else ->
                    throw AssertionError("Unexpected top-level descriptor: $descriptor")
            }

    private fun MemberScope.generateChildStubs(irParent: IrDeclarationContainer) {
        getContributedDescriptors().sortedWith(StableDescriptorsComparator).generateChildStubs (irParent)
    }

    private fun Collection<DeclarationDescriptor>.generateChildStubs(irParent: IrDeclarationContainer) {
        mapTo(irParent.declarations) { generateStub(it) }
    }

    private fun Collection<TypeParameterDescriptor>.generateTypeParameterStubs(irParent: IrTypeParametersContainer) {
        mapTo(irParent.typeParameters) { generateTypeParameterStub(it) }
    }

    private fun Collection<ValueParameterDescriptor>.generateValueParametersStubs(irParent: IrFunction) {
        mapTo(irParent.valueParameters) { generateValueParameterStub(it) }
    }

    private fun generateClassStub(classDescriptor: ClassDescriptor): IrClass =
            context.symbolTable.declareClass(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    classDescriptor
            ).also { irClass ->
                classDescriptor.declaredTypeParameters.generateTypeParameterStubs(irClass)
                classDescriptor.constructors.generateChildStubs(irClass)
                classDescriptor.defaultType.memberScope.generateChildStubs(irClass)
                classDescriptor.staticScope.generateChildStubs(irClass)
            }

    private fun generateEnumEntryStub(enumEntryDescriptor: ClassDescriptor): IrEnumEntry =
            context.symbolTable.declareEnumEntry(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    enumEntryDescriptor
            )

    private fun generateTypeParameterStub(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameter =
            IrTypeParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, typeParameterDescriptor)

    private fun generateValueParameterStub(valueParameterDescriptor: ValueParameterDescriptor): IrValueParameter =
            IrValueParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, valueParameterDescriptor)

    private fun generateConstructorStub(constructorDescriptor: ClassConstructorDescriptor): IrConstructor =
            context.symbolTable.declareConstructor(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    constructorDescriptor
            ).also { irConstructor ->
                constructorDescriptor.valueParameters.generateValueParametersStubs(irConstructor)
            }

    private fun generateFunctionStub(functionDescriptor: FunctionDescriptor): IrFunction =
            context.symbolTable.declareSimpleFunction(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    functionDescriptor
            ).also { irFunction ->
                functionDescriptor.typeParameters.generateTypeParameterStubs(irFunction)
                functionDescriptor.valueParameters.generateValueParametersStubs(irFunction)
            }

    private fun generatePropertyStub(propertyDescriptor: PropertyDescriptor): IrProperty =
            IrPropertyImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    propertyDescriptor
            ).also { irProperty ->
                val getterDescriptor = propertyDescriptor.getter
                if (getterDescriptor == null) {
                    irProperty.backingField =
                            context.symbolTable.declareField(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                                    propertyDescriptor
                            )
                }
                else {
                    irProperty.getter = generateFunctionStub(getterDescriptor)
                }

                irProperty.setter = propertyDescriptor.setter?.let { generateFunctionStub(it) }
            }
}