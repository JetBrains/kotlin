/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.backend.js.generateIrForKlibSerialization
import org.jetbrains.kotlin.ir.backend.js.sortDependencies
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
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
            TargetBackend.JS_IR -> transformToJsIr(module, inputArtifact)
            else -> testServices.assertions.fail { "Target backend ${module.targetBackend} not supported for transformation into IR" }
        }
    }

    private fun transformToJvmIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val files = psiFiles.values.toList()
        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)
        val codegenFactory = JvmIrCodegenFactory(configuration, phaseConfig)
        val state = GenerationState.Builder(
            project, ClassBuilderFactories.TEST, analysisResult.moduleDescriptor, analysisResult.bindingContext,
            files, configuration
        ).isIrBackend(true)
            .ignoreErrors(CodegenTestDirectives.IGNORE_ERRORS in module.directives)
            .diagnosticReporter(DiagnosticReporterFactory.createReporter())
            .build()

        return IrBackendInput.JvmIrBackendInput(
            state,
            codegenFactory,
            codegenFactory.convertToIr(CodegenFactory.IrConversionInput.fromGenerationState(state))
        )
    }

    private fun transformToJsIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val verifySignatures = JsEnvironmentConfigurationDirectives.SKIP_MANGLE_VERIFICATION !in module.directives

        val icData = mutableListOf<KotlinFileSerializedData>()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val moduleFragment = generateIrForKlibSerialization(
            project,
            psiFiles.values.toList(),
            configuration,
            analysisResult,
            sortDependencies(JsEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices)),
            icData,
            expectDescriptorToSymbol,
            IrFactoryImpl,
            verifySignatures
        ) {
            testServices.jsLibraryProvider.getDescriptorByCompiledLibrary(it)
        }

        return IrBackendInput.JsIrBackendInput(
            moduleFragment,
            psiFiles.values.toList(),
            bindingContext = analysisResult.bindingContext,
            icData,
            expectDescriptorToSymbol = expectDescriptorToSymbol,
        )
    }
}
