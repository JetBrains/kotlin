/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WebConfigurationKeys
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

class ClassicFrontend2IrConverter(
    testServices: TestServices
) : Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.ClassicFrontend,
    BackendKinds.IrBackend
) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsLibraryProvider))

    override fun transform(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        return when (module.targetBackend) {
            TargetBackend.JVM_IR -> transformToJvmIr(module, inputArtifact)
            TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> transformToJsIr(module, inputArtifact)
            else -> testServices.assertions.fail { "Target backend ${module.targetBackend} not supported for transformation into IR" }
        }
    }

    private fun transformToJvmIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
        val codegenFactory = JvmIrCodegenFactory(configuration, phaseConfig)
        val state = GenerationState.Builder(
            project, ClassBuilderFactories.TEST, analysisResult.moduleDescriptor, analysisResult.bindingContext,
            configuration
        ).isIrBackend(true)
            .ignoreErrors(CodegenTestDirectives.IGNORE_ERRORS in module.directives)
            .diagnosticReporter(DiagnosticReporterFactory.createReporter())
            .build()

        val conversionResult =
            codegenFactory.convertToIr(CodegenFactory.IrConversionInput.fromGenerationStateAndFiles(state, psiFiles.values))
        return IrBackendInput.JvmIrBackendInput(
            state,
            codegenFactory,
            dependentInputs = emptyList(),
            conversionResult,
            sourceFiles = emptyList()
        )
    }

    private fun transformToJsIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val verifySignatures = JsEnvironmentConfigurationDirectives.SKIP_MANGLE_VERIFICATION !in module.directives

        val sourceFiles = psiFiles.values.toList()
        val icData = configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

        val (moduleFragment, pluginContext) = generateIrForKlibSerialization(
            project,
            sourceFiles,
            configuration,
            analysisResult,
            sortDependencies(JsEnvironmentConfigurator.getAllDependenciesMappingFor(module, testServices)),
            icData,
            expectDescriptorToSymbol,
            IrFactoryImpl,
            verifySignatures
        ) {
            testServices.jsLibraryProvider.getDescriptorByCompiledLibrary(it)
        }

        val errorPolicy = configuration.get(WebConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
        val hasErrors = TopDownAnalyzerFacadeForJSIR.checkForErrors(sourceFiles, analysisResult.bindingContext, errorPolicy)
        val metadataSerializer = KlibMetadataIncrementalSerializer(configuration, project, hasErrors)

        return IrBackendInput.JsIrBackendInput(
            moduleFragment,
            pluginContext,
            sourceFiles.map(::KtPsiSourceFile),
            icData,
            expectDescriptorToSymbol = expectDescriptorToSymbol,
            hasErrors
        ) { file ->
            metadataSerializer.serializeScope(file, analysisResult.bindingContext, moduleFragment.descriptor)
        }
    }
}
