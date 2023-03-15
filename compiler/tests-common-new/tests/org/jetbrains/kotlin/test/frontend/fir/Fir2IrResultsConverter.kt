/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.fir.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.IrBuiltInsOverFir
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
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
    ): IrBackendInput? {
        return try {
            transformInternal(module, inputArtifact)
        } catch (e: Throwable) {
            if (CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS in module.directives && inputArtifact.hasErrors) {
                null
            } else {
                throw e
            }
        }
    }

    private fun transformInternal(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput.JvmIrBackendInput {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl(), JvmIrMangler)

        // Create and initialize the module and its dependencies
        val project = compilerConfigurationProvider.getProject(module)
        // TODO: handle fir from light tree
        val ktFiles = inputArtifact.mainFirFiles.mapNotNull { it.value.psi as KtFile? }
        val sourceFiles = inputArtifact.mainFirFiles.mapNotNull { it.value.sourceFile }
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
            project, ktFiles, NoScopeRecordCliBindingTrace(), configuration,
            compilerConfigurationProvider.getPackagePartProviderFactory(module),
            ::FileBasedDeclarationProviderFactory, CompilerEnvironment,
            TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles), emptyList()
        )

        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)

        val dependentIrParts = mutableListOf<JvmIrCodegenFactory.JvmIrBackendInput>()
        lateinit var mainIrPart: JvmIrCodegenFactory.JvmIrBackendInput
        lateinit var mainModuleComponents: Fir2IrComponents

        val generateSignatures =
            (inputArtifact.partsForDependsOnModules.last().firAnalyzerFacade as? FirAnalyzerFacade)?.generateSignatures == true

        val commonMemberStorage = Fir2IrCommonMemberStorage(
            generateSignatures = generateSignatures,
            signatureComposerCreator = { JvmIdSignatureDescriptor(JvmDescriptorMangler(null)) },
            manglerCreator = { FirJvmKotlinMangler() }
        )
        var irBuiltIns: IrBuiltInsOverFir? = null

        for ((index, firOutputPart) in inputArtifact.partsForDependsOnModules.withIndex()) {
            val (irModuleFragment, components, pluginContext) = firOutputPart.firAnalyzerFacade.convertToIr(
                fir2IrExtensions, commonMemberStorage, irBuiltIns
            )
            irBuiltIns = components.irBuiltIns

            val irPart = JvmIrCodegenFactory.JvmIrBackendInput(
                irModuleFragment,
                components.symbolTable,
                phaseConfig,
                components.irProviders,
                fir2IrExtensions,
                FirJvmBackendExtension(components, irActualizationResult = null),
                pluginContext,
                notifyCodegenStart = {},
            )

            if (index < inputArtifact.partsForDependsOnModules.size - 1) {
                dependentIrParts.add(irPart)
            } else {
                mainModuleComponents = components
                mainIrPart = irPart
            }
        }

        val codegenFactory = JvmIrCodegenFactory(configuration, phaseConfig)
        val generationState = GenerationState.Builder(
            project, ClassBuilderFactories.TEST,
            container.get(), NoScopeRecordCliBindingTrace().bindingContext, configuration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(mainModuleComponents)
        ).build()

        return IrBackendInput.JvmIrBackendInput(
            generationState,
            codegenFactory,
            dependentIrParts,
            mainIrPart,
            sourceFiles
        )
    }
}
