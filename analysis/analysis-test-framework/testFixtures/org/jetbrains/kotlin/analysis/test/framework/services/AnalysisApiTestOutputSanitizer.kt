/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider

/**
 * A test service providing sanitizers for actual / expected test data comparison.
 *
 * These sanitizers are expected to be provided to, e.g., `assertEqualsToFile` calls to ensure the consistency of the test data.
 */
abstract class AnalysisApiTestOutputSanitizer : TestService {
    /**
     * A sanitizer applicable to text rendered by [DebugSymbolRenderer][org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer].
     */
    abstract fun satinizeSymbolDebugRendererOutput(output: String): String
}

/**
 * A sanitizer for multi-platform symbols tests.
 *
 * In most cases, the test results acquired on different platforms are expected to be the same.
 * However, there are still some platform-specific APIs that are rendered in the output, e.g.,
 * [containingJvmClassName][org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent.containingJvmClassName] which
 * returns some meaningful value just for the JVM platform and `null` otherwise. Producing several output files (`.txt` and `.knm.txt` in this case)
 * that differ just in a single line is bulky. [KmpSymbolTestOutputSanitizer] allows applying regular expressions to the output text
 * based on the current target platform.
 *
 * In case of [containingJvmClassName][org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent.containingJvmClassName],
 * the sanitizer replaces all `containingJvmClassName: SomeValue` with `containingJvmClassName: null` for non-JVM platforms when comparing
 * the actual result against the expected one from the test file. This way, we can use a single `.txt` file for all the platforms.
 * The real `.txt` file will have some actual [containingJvmClassName][org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent.containingJvmClassName]
 * value, as this regexp is not applied when running the test on JVM.
 */
class KmpSymbolTestOutputSanitizer(private val testServices: TestServices) : AnalysisApiTestOutputSanitizer() {
    override fun satinizeSymbolDebugRendererOutput(output: String): String =
        @OptIn(TestInfrastructureInternals::class)
        REGEXES.filter { it.shouldRun(testServices.defaultsProvider.targetPlatform) }.fold(output) { acc, sanitizer ->
            acc.replace(sanitizer.regex, sanitizer.replacement)
        }

    companion object {
        private val REGEXES: List<RegexSanitizer> =
            listOf(
                RegexSanitizer(
                    { !it.isJvm() },
                    Regex(
                        """^(\s*getContainingJvmClassName:\s*)\S+$""",
                        RegexOption.MULTILINE
                    ),
                    "$1null"
                )
            )
    }

    private class RegexSanitizer(
        val shouldRun: (TargetPlatform) -> Boolean,
        val regex: Regex,
        val replacement: String,
    )
}

/**
 * Returns [AnalysisApiTestOutputSanitizer] when it's registered in the test configuration,
 * `null` otherwise.
 */
val TestServices.testOutputSanitizer: AnalysisApiTestOutputSanitizer? by TestServices.nullableTestServiceAccessor<AnalysisApiTestOutputSanitizer>()

/**
 * Returns [AnalysisApiTestOutputSanitizer] when it's registered in the test configuration,
 * an identity function otherwise.
 */
val TestServices.testOutputSanitizerOrDefault: (String) -> String
    get() = testOutputSanitizer?.let { it::satinizeSymbolDebugRendererOutput } ?: { it }