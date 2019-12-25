/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.MultifileClassCodegen
import org.jetbrains.kotlin.codegen.PackageCodegen
import org.jetbrains.kotlin.codegen.PackageCodegenImpl
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.factories.IrDeclarationFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.PsiSourceManager

class JvmIrCodegenFactory(
    private val phaseConfig: PhaseConfig,
    private val irDeclarationFactory: IrDeclarationFactory
) : CodegenFactory {

    override fun generateModule(state: GenerationState, files: Collection<KtFile>) {
        JvmBackendFacade.doGenerateFiles(files, state, phaseConfig, irDeclarationFactory)
    }

    fun generateModuleInFrontendIRMode(
        state: GenerationState, irModuleFragment: IrModuleFragment, symbolTable: SymbolTable, sourceManager: PsiSourceManager
    ) {
        val extensions = JvmGeneratorExtensions()
        val irProviders = generateTypicalIrProviderList(
            irModuleFragment.descriptor, irModuleFragment.irBuiltins, symbolTable, extensions = extensions
        )
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
        JvmBackendFacade.doGenerateFilesInternal(
            state, irModuleFragment, symbolTable, sourceManager, phaseConfig, irDeclarationFactory, irProviders, extensions
        )
    }

    override fun createPackageCodegen(state: GenerationState, files: Collection<KtFile>, fqName: FqName): PackageCodegen {
        val impl = PackageCodegenImpl(state, files, fqName)

        return object : PackageCodegen {
            override fun generate() {
                JvmBackendFacade.doGenerateFiles(files, state, phaseConfig, irDeclarationFactory)
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
