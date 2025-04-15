/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForWasm
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.KlibMetadataIncrementalSerializer
import org.jetbrains.kotlin.ir.backend.js.LoadedKlibs
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.incrementalDataProvider
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys

class ClassicFrontend2IrConverter(
    testServices: TestServices
) : Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.ClassicFrontend,
    BackendKinds.IrBackend
) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::LibraryProvider))

    override fun transform(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        return when (val targetBackend = testServices.defaultsProvider.targetBackend) {
            TargetBackend.JVM_IR, TargetBackend.JVM_IR_SERIALIZE -> transformToJvmIr(module, inputArtifact)
            TargetBackend.JS_IR, TargetBackend.JS_IR_ES6 -> transformToJsIr(module, inputArtifact)
            TargetBackend.WASM -> transformToWasmIr(module, inputArtifact)
            else -> testServices.assertions.fail { "Target backend $targetBackend not supported for transformation into IR" }
        }
    }

    private fun transformToJvmIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val codegenFactory = JvmIrCodegenFactory(configuration)
        val state = GenerationState(
            project, analysisResult.moduleDescriptor, configuration, ClassBuilderFactories.TEST,
            ignoreErrors = CodegenTestDirectives.IGNORE_ERRORS in module.directives,
        )

        val backendInput = codegenFactory.convertToIr(state, psiFiles.values, analysisResult.bindingContext)
        return IrBackendInput.JvmIrBackendInput(
            state,
            codegenFactory,
            backendInput,
            sourceFiles = emptyList(),
            descriptorMangler = backendInput.symbolTable.signaturer!!.mangler,
            irMangler = JvmIrMangler,
        )
    }

    private fun transformToJsIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val sourceFiles = psiFiles.values.toList()
        val icData = configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()

        val klibs = LoadedKlibs(all = JsEnvironmentConfigurator.getAllDependenciesMappingFor(module, testServices).keys.toList())

        val (moduleFragment, pluginContext) = generateIrForKlibSerialization(
            project = project,
            files = sourceFiles,
            configuration = configuration,
            analysisResult = analysisResult,
            klibs = klibs,
            icData = icData,
            irFactory = IrFactoryImpl,
        ) {
            testServices.libraryProvider.getDescriptorByCompiledLibrary(it)
        }

        val hasErrors = TopDownAnalyzerFacadeForJSIR.checkForErrors(sourceFiles, analysisResult.bindingContext)
        val metadataSerializer = KlibMetadataIncrementalSerializer(
            sourceFiles,
            configuration,
            project,
            analysisResult.bindingContext,
            moduleFragment.descriptor,
        )
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        return IrBackendInput.JsIrAfterFrontendBackendInput(
            moduleFragment,
            pluginContext,
            icData,
            diagnosticReporter = DiagnosticReporterFactory.createReporter(messageCollector),
            hasErrors,
            descriptorMangler = (pluginContext.symbolTable as SymbolTable).signaturer!!.mangler,
            irMangler = JsManglerIr,
            metadataSerializer = metadataSerializer,
        )
    }

    private fun transformToWasmIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val sourceFiles = psiFiles.values.toList()
        val icData = configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()

        val klibs = LoadedKlibs(all = WasmEnvironmentConfigurator.getAllDependenciesMappingFor(module, testServices).keys.toList())

        val (moduleFragment, pluginContext) = generateIrForKlibSerialization(
            project = project,
            files = sourceFiles,
            configuration = configuration,
            analysisResult = analysisResult,
            klibs = klibs,
            icData = icData,
            irFactory = IrFactoryImpl,
        ) {
            testServices.libraryProvider.getDescriptorByCompiledLibrary(it)
        }

        val analyzerFacade = TopDownAnalyzerFacadeForWasm.facadeFor(configuration.get(WasmConfigurationKeys.WASM_TARGET))
        val hasErrors = analyzerFacade.checkForErrors(sourceFiles, analysisResult.bindingContext)
        val metadataSerializer = KlibMetadataIncrementalSerializer(
            sourceFiles,
            configuration,
            project,
            analysisResult.bindingContext,
            moduleFragment.descriptor,
        )
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        return IrBackendInput.WasmAfterFrontendBackendInput(
            moduleFragment,
            pluginContext,
            icData,
            diagnosticReporter = DiagnosticReporterFactory.createReporter(messageCollector),
            hasErrors,
            descriptorMangler = (pluginContext.symbolTable as SymbolTable).signaturer!!.mangler,
            irMangler = JsManglerIr,
            metadataSerializer = metadataSerializer,
        )
    }
}
