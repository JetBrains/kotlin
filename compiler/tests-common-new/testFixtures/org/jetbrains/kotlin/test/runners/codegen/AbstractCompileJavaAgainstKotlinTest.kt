/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.JavaCompilationHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_JAVA_FACADE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerJvmTest

/**
 * Compiles the Kotlin part of a test module, then compiles its `.java` files with real javac against the
 * resulting Kotlin classes — checking that Java code interoperating with the given Kotlin declarations
 * actually compiles.
 *
 * If a `<testName>.javaerr.txt` golden file exists, javac is instead expected to fail, and its diagnostics
 * are compared against that file (see [org.jetbrains.kotlin.test.backend.handlers.JavaCompilationHandler]).
 */
open class AbstractCompileJavaAgainstKotlinTest : AbstractKotlinCompilerJvmTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +WITH_STDLIB
            +WITH_REFLECT
            +DISABLE_JAVA_FACADE
        }

        setupJvmPipelineSteps(FirParser.LightTree)
        commonHandlersForCodegenTest()

        configureJvmArtifactsHandlersStep {
            useHandlers(::JavaCompilationHandler)
        }
    }
}
