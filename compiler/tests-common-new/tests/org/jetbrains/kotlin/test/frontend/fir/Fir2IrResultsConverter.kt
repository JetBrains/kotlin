/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class Fir2IrResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val extensions = JvmGeneratorExtensionsImpl()
        val (irModuleFragment, symbolTable, components) = inputArtifact.firAnalyzerFacade.convertToIr(extensions)
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases)
        val codegenFactory = JvmIrCodegenFactory(phaseConfig)

        // TODO: handle fir from light tree
        val ktFiles = inputArtifact.firFiles.values.mapNotNull { it.psi as KtFile? }

        // Create and initialize the module and its dependencies
        val project = compilerConfigurationProvider.getProject(module)
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
            project, ktFiles, NoScopeRecordCliBindingTrace(), configuration,
            compilerConfigurationProvider.getPackagePartProviderFactory(module),
            ::FileBasedDeclarationProviderFactory, CompilerEnvironment,
            TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles), emptyList()
        )

        val generationState = GenerationState.Builder(
            project, ClassBuilderFactories.TEST,
            container.get(), dummyBindingContext, ktFiles,
            configuration
        ).codegenFactory(
            codegenFactory
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(components)
        ).build()

        irModuleFragment.irBuiltins.functionFactory = IrFunctionFactory(irModuleFragment.irBuiltins, symbolTable)
        val irProviders = codegenFactory.configureBuiltInsAndGenerateIrProvidersInFrontendIRMode(irModuleFragment, symbolTable, extensions)

        return IrBackendInput(
            JvmIrCodegenFactory.JvmIrBackendInput(
                generationState,
                irModuleFragment,
                symbolTable,
                phaseConfig,
                irProviders,
                extensions,
                FirJvmBackendExtension(inputArtifact.session, components),
            ) {}
        )
    }
}
