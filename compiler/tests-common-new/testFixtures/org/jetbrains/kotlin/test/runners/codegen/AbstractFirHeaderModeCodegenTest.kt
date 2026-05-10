/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.configureJvmBoxCodegenSettings
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_HEADER_MODE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.HEADER_MODE
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractFirHeaderModeCodegenTestBase(
    val parser: FirParser
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        defaultDirectives {
            +HEADER_MODE
        }

        setupJvmPipelineSteps(parser)
        useFailureSuppressors(
            { BlackBoxCodegenSuppressor(it, customIgnoreDirective = IGNORE_HEADER_MODE) },
        )

        configureJvmBoxCodegenSettings(includeAllDumpHandlers = false, includeBytecodeTextHandler = false)
    }
}

open class AbstractFirLightTreeHeaderModeCodegenTest : AbstractFirHeaderModeCodegenTestBase(FirParser.LightTree)
