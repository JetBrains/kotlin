/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

/**
 * Encapsulates test data file paths and mode for a single assertion.
 *
 * Given a test input file (e.g., `test.kt`), a variant chain, and an extension,
 * this class resolves the ordered list of candidate output files from most specific to least specific.
 *
 * For example, with `testDataPath = test.kt`, `variantChain = ["knm", "js"]`, `extension = ".txt"`:
 * - [readableFiles] = `[test.js.txt, test.knm.txt, test.txt]`
 * - [writeTargetFile] = `test.js.txt` (most specific, always written to on create/update)
 * - [readTargetFile] = first file in [readableFiles] that exists
 * - [fallbackFile] = first existing file after [writeTargetFile]
 *
 * Used as a context parameter in [ManagedTestAssertions] helpers, replacing explicit threading of
 * `testDataFiles` and `mode` through every call.
 */
internal class TestDataContext private constructor(
    /** The original test input file path (e.g., `test.kt`). */
    val testDataPath: Path,

    /**
     * Candidate output files ordered from most specific (first) to least specific (last).
     * The first element is always the [writeTargetFile].
     */
    val readableFiles: List<Path>,

    /** The variant chain used to build this instance. */
    val variantChain: TestVariantChain,

    /** The test data manager mode for this assertion. */
    val mode: TestDataManagerMode,
) {
    /** The most specific file — always the target for write operations (create, update, delete). */
    val writeTargetFile: Path
        get() = readableFiles.first()

    /** The first existing file in [readableFiles], or `null` if none exist. */
    val readTargetFile: Path?
        get() = readableFiles.firstOrNull { it.exists() }

    /** The first existing file after [writeTargetFile]. */
    val fallbackFile: Path?
        get() = readableFiles.drop(1).firstOrNull { it.exists() }

    companion object {
        fun build(
            testDataPath: Path,
            variantChain: TestVariantChain,
            extension: String,
            mode: TestDataManagerMode,
        ): TestDataContext {
            val baseName = testDataPath.nameWithoutExtension
            val directory = testDataPath.parent
            val ext = "." + extension.removePrefix(".")

            val readableFiles = buildList {
                for (variant in variantChain.asReversed()) {
                    add(directory.resolve("$baseName.$variant$ext"))
                }

                add(directory.resolve("$baseName$ext"))
            }

            return TestDataContext(testDataPath, readableFiles, variantChain, mode)
        }
    }
}