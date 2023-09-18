/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.IrBuiltInsOverFir
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.pipeline.signatureComposerForJvmFir2Ir
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
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

class Fir2IrJvmResultsConverter(
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

        val irMangler = JvmIrMangler
        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl(), irMangler)

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

        val dependentIrParts = mutableListOf<IrModuleFragment>()
        lateinit var backendInput: JvmIrCodegenFactory.JvmIrBackendInput
        lateinit var mainModuleComponents: Fir2IrComponents

        val generateSignatures = compilerConfigurationProvider.getCompilerConfiguration(module)
            .getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES)
        val commonMemberStorage = Fir2IrCommonMemberStorage(signatureComposerForJvmFir2Ir(generateSignatures), FirJvmKotlinMangler())
        var irBuiltIns: IrBuiltInsOverFir? = null
        val diagnosticReporter = DiagnosticReporterFactory.createReporter()

        for ((index, firOutputPart) in inputArtifact.partsForDependsOnModules.withIndex()) {
            val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(module)
            val fir2IrConfiguration = Fir2IrConfiguration(
                languageVersionSettings = module.languageVersionSettings,
                diagnosticReporter = diagnosticReporter,
                linkViaSignatures = compilerConfiguration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES),
                evaluatedConstTracker = compilerConfiguration
                    .putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create()),
                inlineConstTracker = compilerConfiguration[CommonConfigurationKeys.INLINE_CONST_TRACKER],
                expectActualTracker = compilerConfiguration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
                allowNonCachedDeclarations = false,
                useIrFakeOverrideBuilder = module.shouldUseIrFakeOverrideBuilderInConvertToIr(),
            )
            val (irModuleFragment, components, pluginContext) = firOutputPart.firAnalyzerFacade.result.outputs.single().convertToIr(
                fir2IrExtensions,
                fir2IrConfiguration,
                commonMemberStorage,
                irBuiltIns,
                irMangler,
                FirJvmVisibilityConverter,
                DefaultBuiltIns.Instance,
                typeContextProvider = ::JvmIrTypeSystemContext,
                )
            irBuiltIns = components.irBuiltIns

            if (index < inputArtifact.partsForDependsOnModules.size - 1) {
                dependentIrParts.add(irModuleFragment)
            } else {
                mainModuleComponents = components
                backendInput = JvmIrCodegenFactory.JvmIrBackendInput(
                    irModuleFragment,
                    components.symbolTable,
                    phaseConfig,
                    components.irProviders,
                    fir2IrExtensions,
                    FirJvmBackendExtension(components, irActualizedResult = null),
                    pluginContext,
                    notifyCodegenStart = {},
                )
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
        ).diagnosticReporter(
            diagnosticReporter
        ).build()

        val result = IrBackendInput.JvmIrBackendInput(
            generationState,
            codegenFactory,
            backendInput,
            dependentIrParts,
            sourceFiles,
            descriptorMangler = commonMemberStorage.symbolTable.signaturer.mangler,
            irMangler = irMangler,
            firMangler = commonMemberStorage.firSignatureComposer.mangler,
            fir2IrComponents = mainModuleComponents
        )

        return result
    }
}
