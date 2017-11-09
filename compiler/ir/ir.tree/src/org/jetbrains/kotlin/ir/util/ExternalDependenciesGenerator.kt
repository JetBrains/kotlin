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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class ExternalDependenciesGenerator(val symbolTable: SymbolTable, val irBuiltIns: IrBuiltIns) {
    private val stubGenerator = DeclarationStubGenerator(symbolTable, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB)

    fun generateUnboundSymbolsAsDependencies(irModule: IrModuleFragment) {
        val collector = DependenciesCollector()
        collector.collectTopLevelDescriptorsForUnboundSymbols(symbolTable)

        collector.dependencyModules.mapTo(irModule.dependencyModules) { moduleDescriptor ->
            generateModuleStub(collector, moduleDescriptor)
        }
    }

    private fun generateModuleStub(collector: DependenciesCollector, moduleDescriptor: ModuleDescriptor): IrModuleFragment =
            stubGenerator.generateEmptyModuleFragmentStub(moduleDescriptor, irBuiltIns).also { irDependencyModule ->
                collector.getPackageFragments(moduleDescriptor).mapTo(irDependencyModule.externalPackageFragments) { packageFragmentDescriptor ->
                    generatePackageStub(packageFragmentDescriptor, collector.getTopLevelDescriptors(packageFragmentDescriptor))
                }
            }

    private fun generatePackageStub(packageFragmentDescriptor: PackageFragmentDescriptor, topLevelDescriptors: Collection<DeclarationDescriptor>): IrExternalPackageFragment =
            stubGenerator.generateEmptyExternalPackageFragmentStub(packageFragmentDescriptor).also { irExternalPackageFragment ->
                topLevelDescriptors.mapTo(irExternalPackageFragment.declarations) {
                    stubGenerator.generateMemberStub(it)
                }
            }

}