/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
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
    ) = when (module.targetBackend) {
        TargetBackend.JVM_IR -> transformAsJvm(module, inputArtifact)
        TargetBackend.JS_IR -> transformAsJs(module, inputArtifact)
        else -> testServices.assertions.fail { "Target backend ${module.targetBackend} not supported for transformation into IR" }
    }

    private fun transformAsJvm(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)
        val extensions = JvmGeneratorExtensionsImpl(configuration)

        val (irModuleFragment, symbolTable, components) = inputArtifact.firAnalyzerFacade.convertToIr(extensions)
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
        val codegenFactory = JvmIrCodegenFactory(configuration, phaseConfig, jvmGeneratorExtensions = extensions)

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

        val irProviders = codegenFactory.configureBuiltInsAndGenerateIrProvidersInFrontendIRMode(irModuleFragment, symbolTable, extensions)

        return IrBackendInput.JvmIrBackendInput(
            generationState,
            JvmIrCodegenFactory.JvmIrBackendInput(
                irModuleFragment,
                symbolTable,
                phaseConfig,
                irProviders,
                extensions,
                FirJvmBackendExtension(inputArtifact.session, components),
                notifyCodegenStart = {},
            )
        )
    }

    @Suppress("UNUSED_VARIABLE")
    private fun transformAsJs(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)
        val extensions = GeneratorExtensions()

        val (irModuleFragment, symbolTable, components) = inputArtifact.firAnalyzerFacade.convertToIr(extensions)
        val dummyBindingContext = NoScopeRecordCliBindingTrace().bindingContext

        // TODO: handle fir from light tree
        val ktFiles = inputArtifact.allFirFiles.values.mapNotNull { it.psi as KtFile? }

        // Create and initialize the module and its dependencies
        val project = compilerConfigurationProvider.getProject(module)

        val verifySignatures = JsEnvironmentConfigurationDirectives.SKIP_MANGLE_VERIFICATION !in module.directives

        val icData = mutableListOf<KotlinFileSerializedData>()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

        return IrBackendInput.JsIrBackendInput(
            irModuleFragment,
            ktFiles,
            dummyBindingContext,
            icData,
            expectDescriptorToSymbol,
            inputArtifact.firAnalyzerFacade.scopeSession,
            components,
            firFiles = inputArtifact.allFirFiles.map { it.value },
        )
    }
}
