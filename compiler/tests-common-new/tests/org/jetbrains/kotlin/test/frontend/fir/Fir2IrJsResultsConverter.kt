/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.convertToJsIr
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.incrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.serializeSingleFirFile
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class Fir2IrJsResultsConverter(
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
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val fir2IrExtensions = Fir2IrExtensions.Default
        val firFiles = inputArtifact.firAnalyzerFacade.runResolution()
        val (irModuleFragment, components) = inputArtifact.firAnalyzerFacade.convertToJsIr(firFiles, fir2IrExtensions)

        val sourceFiles = firFiles.mapNotNull { it.sourceFile }
        val firFilesBySourceFile = firFiles.associateBy { it.sourceFile }

        val icData = configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                ?: GenerationState.LANGUAGE_TO_METADATA_VERSION.getValue(module.languageVersionSettings.languageVersion)

        return IrBackendInput.JsIrBackendInput(
            irModuleFragment,
            sourceFiles,
            icData,
            expectDescriptorToSymbol,
            hasErrors = false // TODO: implement error check
        ) { file ->
            val firFile = firFilesBySourceFile[file] ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
            serializeSingleFirFile(firFile, components.session, components.scopeSession, metadataVersion)
        }
    }
}

