/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

/**
 * Enables `java-direct` for `JavaUsingAst*` tests.
 */
internal class JavaDirectConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule,
    ) {
        super.configureCompilerConfiguration(configuration, module)

        configuration.put(JVMConfigurationKeys.USE_JAVA_DIRECT, true)
    }
}

class OnlyTestsWithJavaSourcesMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean =
        testServices.moduleStructure.modules.none { module -> module.files.any { it.isJavaFile } }
}

