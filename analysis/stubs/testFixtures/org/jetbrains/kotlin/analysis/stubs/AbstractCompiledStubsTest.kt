/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives

/**
 * This test is supposed to validate the compiled stubs output.
 *
 * It takes a compiled file as a binary data and creates stubs for it.
 *
 * @see org.jetbrains.kotlin.analysis.decompiler.psi.AbstractDecompiledTextTest
 */
abstract class AbstractCompiledStubsTest(defaultTargetPlatform: TargetPlatform) : AbstractStubsTest() {
    override val outputFileExtension: String get() = "compiled.stubs.txt"
    override val configurator: AnalysisApiTestConfigurator = CompiledStubsTestConfigurator(defaultTargetPlatform)
    override val stubsTestEngine: StubsTestEngine get() = CompiledStubsTestEngine

    internal open class CompiledStubsTestConfigurator(
        override val defaultTargetPlatform: TargetPlatform,
    ) : AnalysisApiFirBinaryTestConfigurator() {
        override val testModuleFactory: KtTestModuleFactory
            get() = KtLibraryBinaryDecompiledTestModuleFactory

        override val testPrefixes: List<String>
            get() {
                val simplePlatform = defaultTargetPlatform.singleOrNull()
                val additionalNames = buildList {
                    if (simplePlatform != null) {
                        add(simplePlatform.platformName)
                    } else {
                        add("Common")
                    }

                    // All supported platforms except for the JVM might be compiled as .knm files,
                    // so their output should be the same in most cases
                    if (simplePlatform !is JvmPlatform) {
                        add("knm")
                    }
                }

                return additionalNames + super.testPrefixes
            }

        override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
            super.configureTest(builder, disposable)

            // Drop together with the test data as soon as 1.9 is unsupported
            builder.forTestsMatching("*/k1/*") {
                defaultDirectives {
                    LanguageSettingsDirectives.LANGUAGE_VERSION with LanguageVersion.KOTLIN_1_9
                    LanguageSettingsDirectives.API_VERSION with ApiVersion.KOTLIN_1_9
                    +LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
                }
            }
        }
    }
}
