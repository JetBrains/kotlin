/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir.interpreter

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrInterpreterBackendHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.preprocessors.IrInterpreterImplicitKotlinImports
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.IrInterpreterHelpersSourceFilesProvider

open class AbstractIrInterpreterAfterFir2IrTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            artifactKind = BinaryKind.NoArtifact
            targetBackend = TargetBackend.JVM_IR
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
            +JvmEnvironmentConfigurationDirectives.NO_RUNTIME
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::IrInterpreterEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(::IrInterpreterHelpersSourceFilesProvider)
        useSourcePreprocessor(::IrInterpreterImplicitKotlinImports)

        useFrontendFacades(::FirFrontendFacade)
        useFrontend2BackendConverters(::Fir2IrResultsConverter)
        useBackendFacades(::JvmIrBackendFacade)
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

        useBackendHandlers(::IrInterpreterBackendHandler)
        enableMetaInfoHandler()
    }
}

class IrInterpreterEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return super.provideAdditionalAnalysisFlags(directives, languageVersion) + (AnalysisFlags.builtInsFromSources to true)
    }
}