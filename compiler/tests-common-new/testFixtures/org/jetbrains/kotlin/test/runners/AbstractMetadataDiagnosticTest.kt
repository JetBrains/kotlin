/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.METADATA_ONLY_COMPILATION
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataSerializerFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.MetadataEnvironmentConfigurator

abstract class AbstractMetadataDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = CommonPlatforms.defaultCommonPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            FIR_PARSER with FirParser.LightTree
            +METADATA_ONLY_COMPILATION
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::MetadataEnvironmentConfigurator,
        )

        facadeStep(::FirCliMetadataFrontendFacade)

        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        facadeStep(::FirCliMetadataSerializerFacade)

        enableMetaInfoHandler()
    }
}