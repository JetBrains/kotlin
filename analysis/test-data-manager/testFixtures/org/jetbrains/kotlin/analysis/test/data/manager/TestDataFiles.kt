/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

/**
 * Encapsulates test data file paths for a single test.
 *
 * The [writeTargetFile] is always the first (most specific) file in [readableFiles].
 */
internal class TestDataFiles private constructor(val readableFiles: List<Path>) {
    val writeTargetFile: Path
        get() = readableFiles.first()

    val firstNonWritableFileIfExists: Path?
        get() = readableFiles.drop(1).firstOrNull { it.exists() }

    companion object {
        fun build(testDataPath: Path, variantChain: TestVariantChain, extension: String): TestDataFiles {
            val baseName = testDataPath.nameWithoutExtension
            val directory = testDataPath.parent
            val ext = "." + extension.removePrefix(".")

            val readableFiles = buildList {
                for (variant in variantChain.asReversed()) {
                    add(directory.resolve("$baseName.$variant$ext"))
                }

                add(directory.resolve("$baseName$ext"))
            }

            return TestDataFiles(readableFiles)
        }
    }
}