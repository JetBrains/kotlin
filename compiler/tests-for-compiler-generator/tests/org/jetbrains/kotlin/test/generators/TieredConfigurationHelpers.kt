/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.test.directives.TestTierDirectives
import org.jetbrains.kotlin.test.runners.TestTierLabel
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import java.io.File

private val TIERED_DIRECTIVE = "// ${TestTierDirectives.RUN_PIPELINE_TILL}:"
private val TIERED_OVERRIDE_DIRECTIVE = "// ${TestTierDirectives.TARGET_RUNNER_TIER}:"

fun File.shouldBeRunByRunnerOf(vararg tiers: TestTierLabel): Boolean {
    val lines = readLines()
    val directiveParts = lines.find { it.startsWith(TIERED_OVERRIDE_DIRECTIVE) }?.split(TIERED_OVERRIDE_DIRECTIVE)
        ?: lines.find { it.startsWith(TIERED_DIRECTIVE) }?.split(TIERED_DIRECTIVE)

    val declaredTier = directiveParts
        ?.getOrNull(1)?.trim()?.let(TestTierLabel::valueOf)
        ?: TestTierLabel.FRONTEND

    val minDeclaredTier = tiers.min()
    val maxDeclaredTier = tiers.max()

    return declaredTier in minDeclaredTier..maxDeclaredTier
}

/**
 * Configures test runners whose generation relies on the values
 * of [// RUN_PIPELINE_TILL][TestTierDirectives.RUN_PIPELINE_TILL]
 * in test files
 */
fun configureTierModelsForDeclaredAs(
    vararg tiers: TestTierLabel,
    relativeRootPaths: List<String>,
    excludeDirs: List<String>,
    excludeDirsRecursively: List<String> = emptyList(),
    extension: String? = "kt",
    pattern: String = if (extension == null) """^([^\.]+)$""" else "^(.+)\\.$extension\$",
    excludedPattern: String? = null,
): TestGroup.TestClass.() -> Unit = {
    for (path in relativeRootPaths) {
        model(
            path,
            excludeDirs = excludeDirs,
            skipSpecificFile = { !it.shouldBeRunByRunnerOf(*tiers) },
            skipTestAllFilesCheck = true,
            generateEmptyTestClasses = false,
            extension = extension,
            pattern = pattern,
            excludedPattern = excludedPattern,
            excludeDirsRecursively = excludeDirsRecursively,
        )
    }
}

/**
 * [...][configureTierModelsForDeclaredAs] for the common locations of diagnostic tests
 */
fun configureTierModelsForK1AlongsideDiagnosticTestsStating(
    vararg tiers: TestTierLabel,
    allowKts: Boolean,
    excludeDirsRecursively: List<String> = emptyList(),
): TestGroup.TestClass.() -> Unit =
    configureTierModelsForDeclaredAs(
        *tiers,
        relativeRootPaths = listOf(
            "testData/diagnostics/tests",
            "testData/diagnostics/testsWithStdLib",
            "fir/analysis-tests/testData/resolve",
            "fir/analysis-tests/testData/resolveWithStdlib",
            // Those files might contain code which when being analyzed in the IDE might accidentally freeze it, thus we use a fake
            // file extension `nkt` for it.
            "fir/analysis-tests/testData/resolveFreezesIDE",
        ),
        excludeDirs = listOf("declarations/multiplatform/k1"),
        pattern = when {
            allowKts -> "^(.*)\\.(kts?|nkt)$"
            else -> "^(.*)\\.(kt|nkt)$"
        },
        excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
        excludeDirsRecursively = excludeDirsRecursively,
    )
