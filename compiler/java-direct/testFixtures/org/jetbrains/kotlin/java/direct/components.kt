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

private val javaFileRegex = Regex("""^\s*//\s* FILE:\s* .*\.java\s*$""")

class OnlyTestsWithJavaSourcesMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean =
        testServices.moduleStructure.originalTestDataFiles.first().useLines { lines -> lines.none { it.matches(javaFileRegex) } }
}

/**
 * Shared diagnostics tests whose expectations pin the PSI Java model's loose handling of a
 * package/type name clash (JLS 6.1): PSI falls back to the package interpretation where javac
 * commits to the shadowing type and reports an error. `java-direct` follows javac, so these
 * tests are skipped here and mirrored by javac-strict copies under
 * `compiler/java-direct/testData/diagnostics` (KT-87813).
 */
private val testsPinningPsiJavaModelDeviations = setOf(
    "compiler/testData/diagnostics/tests/javac/qualifiedExpression/PackageVsClass2.kt",
)

class SkipTestsPinningPsiJavaModelDeviationsMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val path = testServices.moduleStructure.originalTestDataFiles.first().invariantSeparatorsPath
        return testsPinningPsiJavaModelDeviations.any { path.endsWith(it) }
    }
}

