/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.MultifileClassCodegen
import org.jetbrains.kotlin.codegen.PackageCodegen
import org.jetbrains.kotlin.codegen.PackageCodegenImpl
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.PsiSourceManager

class JvmIrCodegenFactory(private val phaseConfig: PhaseConfig) : CodegenFactory {

    override fun generateModule(state: GenerationState, files: Collection<KtFile>) {
        JvmBackendFacade.doGenerateFiles(files, state, phaseConfig)
    }

    fun generateModuleInFrontendIRMode(
        state: GenerationState,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        sourceManager: PsiSourceManager,
        createCodegen: (IrClass, JvmBackendContext, IrFunction?) -> ClassCodegen,
    ) {
        irModuleFragment.irBuiltins.functionFactory = IrFunctionFactory(irModuleFragment.irBuiltins, symbolTable)
        val extensions = JvmGeneratorExtensions()
        val irProviders = generateTypicalIrProviderList(
            irModuleFragment.descriptor, irModuleFragment.irBuiltins, symbolTable, extensions = extensions
        )

        JvmBackendFacade.doGenerateFilesInternal(
            state, irModuleFragment, symbolTable, sourceManager, phaseConfig, irProviders, extensions, createCodegen
        )
    }

    override fun createPackageCodegen(state: GenerationState, files: Collection<KtFile>, fqName: FqName): PackageCodegen {
        val impl = PackageCodegenImpl(state, files, fqName)

        return object : PackageCodegen {
            override fun generate() {
                JvmBackendFacade.doGenerateFiles(files, state, phaseConfig)
            }

            override fun getPackageFragment(): PackageFragmentDescriptor {
                return impl.packageFragment
            }
        }
    }

    override fun createMultifileClassCodegen(state: GenerationState, files: Collection<KtFile>, fqName: FqName): MultifileClassCodegen {
        TODO()
    }
}
