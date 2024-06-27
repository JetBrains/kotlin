/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiJvmEnvironmentConfigurator
import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

fun TestConfigurationBuilder.configureLibraryCompilationSupport() {
    useAdditionalService<TestModuleCompiler> { DispatchingTestModuleCompiler }
    useAdditionalService<TestModuleDecompiler> { TestModuleDecompilerJar() }
    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::AnalysisApiJvmEnvironmentConfigurator,
        ::JsEnvironmentConfigurator
    )
}

class LibraryWasNotCompiledDueToExpectedCompilationError : SkipTestException()
