/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrPreSerializationSymbolValidationHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.junit.jupiter.api.Test
import java.nio.file.Files

abstract class AbstractSymbolsValidationTextTest(
    targetBackend: TargetBackend,
    private val targetPlatform: TargetPlatform,
    private val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>,
    private val frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>,
    private val irInliningFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>,
    private val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>,
    private val deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>,
    private val handler: Constructor<IrPreSerializationSymbolValidationHandler>
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    open fun TestConfigurationBuilder.applyConfigurators() {}

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = this@AbstractSymbolsValidationTextTest.targetPlatform
            targetBackend = this@AbstractSymbolsValidationTextTest.targetBackend
            artifactKind = ArtifactKind.NoArtifact
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }

        useAdditionalService(::LibraryProvider)

        facadeStep(frontendFacade)
        firHandlersStep()
        facadeStep(frontendToIrConverter)
        irHandlersStep {
            useHandlers(handler)
        }
        facadeStep(irInliningFacade)
        loweredIrHandlersStep()

        enableMetaInfoHandler()
        facadeStep(serializerFacade)
        facadeStep(deserializerFacade)
        deserializedIrHandlersStep {
//            useHandlers(handler) // TODO create a new handler
        }

        configureFirParser(FirParser.LightTree)

        useConfigurators(::CommonEnvironmentConfigurator)
        applyConfigurators()
    }
}

abstract class AbstractPreSerializationSymbolsTest(
    targetBackend: TargetBackend,
    targetPlatform: TargetPlatform,
    frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>,
    frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>,
    irInliningFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>,
    serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>,
    deserializerFacade: Constructor<DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>,
    handler: Constructor<IrPreSerializationSymbolValidationHandler>,
) : AbstractSymbolsValidationTextTest(
    targetBackend,
    targetPlatform,
    frontendFacade,
    frontendToIrConverter,
    irInliningFacade,
    serializerFacade,
    deserializerFacade,
    handler
) {
    @Test
    fun testValidation() {
        val file = Files.createTempFile("validation", ".kt").toFile()
        file.writeText("fun main() {}")
        runTest(file.absolutePath)
    }
}