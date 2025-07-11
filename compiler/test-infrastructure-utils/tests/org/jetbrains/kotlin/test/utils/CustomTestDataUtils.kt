/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

// Prefixes are chosen such that LL FIR test data cannot be mistaken for FIR test data.
private const val FIR_PREFIX = ".fir"
private const val LATEST_LV_PREFIX = ".latestLV"
private const val LL_FIR_PREFIX = ".ll"
private const val REVERSED_PREFIX = ".reversed"
private const val PARTIAL_BODY_PREFIX = ".partialBody"

const val CUSTOM_TEST_DATA_EXTENSION_PATTERN = "^(.+)\\.(reversed|partialBody|fir|ll|latestLV)\\.kts?\$"

val Path.isFirTestData: Boolean
    get() = isCustomTestDataWithPrefix(FIR_PREFIX)

val Path.isLatestLVTestData: Boolean
    get() = isCustomTestDataWithPrefix(LATEST_LV_PREFIX)

/**
 * @see Path.llFirTestDataFile
 */
val Path.isLLFirTestData: Boolean
    get() = isCustomTestDataWithPrefix(LL_FIR_PREFIX)

val Path.isLLFirSpecializedTestData: Boolean
    get() = isCustomTestDataWithPrefix(REVERSED_PREFIX) || isCustomTestDataWithPrefix(PARTIAL_BODY_PREFIX)

val Path.isCustomTestData: Boolean
    get() = isFirTestData || isLLFirTestData || isLatestLVTestData

private fun Path.isCustomTestDataWithPrefix(prefix: String): Boolean = name.endsWith("$prefix$extensionWithDot")

val Path.firTestDataFile: Path
    get() = getCustomTestDataFileWithPrefix(FIR_PREFIX)

val Path.latestLVTestDataFile: Path
    get() = getCustomTestDataFileWithPrefix(LATEST_LV_PREFIX)

val Path.reversedTestDataFile: Path
    get() = getCustomTestDataFileWithPrefix(REVERSED_PREFIX)

val Path.partialBodyTestDataFile: Path
    get() = getCustomTestDataFileWithPrefix(PARTIAL_BODY_PREFIX)

/**
 * An LL FIR test data file (`.ll.kt`) allows tailoring the expected output of a test to the LL FIR case. In very rare cases, LL FIR may
 * legally diverge from the output of the K2 compiler, such as when the compiler's error behavior is deliberately unspecified. (For an
 * example, see `kotlinJavaKotlinCycle.ll.kt`.)
 */
val Path.llFirTestDataFile: Path
    get() = getCustomTestDataFileWithPrefix(LL_FIR_PREFIX)

private fun Path.getCustomTestDataFileWithPrefix(prefix: String): Path =
    if (isCustomTestDataWithPrefix(prefix)) this
    else {
        // Because `File` can be `.ll.kt` or `.fir.kt` test data, we have to go off `originalTestDataFileName`, which removes the prefix
        // intelligently.
        val originalName = originalTestDataFileName
        val extension = extensionWithDot
        val customName = "${originalName.removeSuffix(extension)}$prefix$extension"
        parent.resolve(customName)
    }

val Path.originalTestDataFile: Path
    get() {
        val originalName = originalTestDataFileName
        return if (originalName != name) parent.resolve(originalName) else this
    }

val Path.originalTestDataFileName: String
    get() {
        val prefix = when {
            isLLFirTestData -> LL_FIR_PREFIX
            isFirTestData -> FIR_PREFIX
            isLatestLVTestData -> LATEST_LV_PREFIX
            else -> return name
        }
        return getOriginalTestDataFileNameFromPrefix(prefix)
    }

private fun Path.getOriginalTestDataFileNameFromPrefix(prefix: String): String {
    val extension = extensionWithDot
    return "${name.removeSuffix("$prefix$extension")}$extension"
}

private val Path.extensionWithDot: String get() = ".$extension"