/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import java.io.File

// The variants of test data: the part of a file name before its extension, e.g., `foo.ll.kt` is the `ll` variant of `foo.kt`.
// Variants are chosen such that LL FIR test data cannot be mistaken for FIR test data.
const val LATEST_LV_VARIANT = "latestLV"
const val LF_DISABLED_VARIANT = "disabled"
const val LL_FIR_VARIANT = "ll"
const val REVERSED_VARIANT = "reversed"
const val PARTIAL_BODY_VARIANT = "partialBody"
const val CONTEXT_SENSITIVE_VARIANT = "contextSensitive"
const val NO_ALIAS_EXPANSION_VARIANT = "noAliasExpansion"
const val WASM_JS_VARIANT = "wasmJs"
const val WASM_WASI_VARIANT = "wasmWasi"

private const val LATEST_LV_PREFIX = ".$LATEST_LV_VARIANT"
private const val LF_DISABLED_PREFIX = ".$LF_DISABLED_VARIANT"
private const val LL_FIR_PREFIX = ".$LL_FIR_VARIANT"

/**
 * The script kinds whose test data has a compound extension: `foo.repl.kts` for the REPL and `foo.test.kts` for the custom test script
 * definition (`ConfigurableTestScript`). A variant of such test data goes before the whole extension, e.g., `foo.ll.test.kts`, so that
 * the variant is the same kind of script (see [extensionWithDot]).
 */
private val COMPOUND_SCRIPT_KINDS: List<String> = listOf("repl", "test")

private val COMPOUND_EXTENSIONS: List<String> = COMPOUND_SCRIPT_KINDS.map { ".$it.kts" }

/**
 * The variant of test data of [this] variant on a Wasm target, e.g., `ll.wasmJs` for `ll` and [WASM_JS_VARIANT].
 */
fun String.withWasmVariant(wasmVariant: String): String = "$this.$wasmVariant"

private val WASM_VARIANTS: List<String> = listOf(WASM_JS_VARIANT, WASM_WASI_VARIANT)

/**
 * The variants of test data which repeat its code with the diagnostics of another test configuration, see [withTestDataVariants].
 * `.disabled.kt` test data is not one of them, as it may declare other directives.
 */
private val TEST_DATA_VARIANTS: List<String> = buildList {
    add(LL_FIR_VARIANT)
    add(LATEST_LV_VARIANT)
    add(REVERSED_VARIANT)
    add(PARTIAL_BODY_VARIANT)
    add(CONTEXT_SENSITIVE_VARIANT)
    add(NO_ALIAS_EXPANSION_VARIANT)
    for (wasmVariant in WASM_VARIANTS) {
        add(LL_FIR_VARIANT.withWasmVariant(wasmVariant))
        add(REVERSED_VARIANT.withWasmVariant(wasmVariant))
    }
}

/**
 * The variants of LL FIR test data which diverge from the compiler's test data and need an explanation: `.ll.kt` (see
 * [llFirTestDataFile]) and its Wasm variants, e.g., `.ll.wasmJs.kt`.
 */
private val LL_FIR_DIVERGENCE_VARIANTS: List<String> = listOf(LL_FIR_VARIANT) + WASM_VARIANTS.map { LL_FIR_VARIANT.withWasmVariant(it) }

/**
 * This base test data file and its existing variants which repeat its code, e.g., `foo.kt`, `foo.ll.kt` and `foo.reversed.kt`. A directive
 * or a comment added to the base test data has to be added to all of them.
 */
val File.withTestDataVariants: List<File>
    get() {
        val extension = extensionWithDot
        val baseName = name.removeSuffix(extension)
        return (listOf(this) + TEST_DATA_VARIANTS.map { parentFile.resolve("$baseName.$it$extension") }).filter { it.exists() }
    }

/**
 * The variants whose tests check their own phase, so the phase of the test data isn't copied to them, see [withPhaseSharingTestDataVariants].
 */
private val OWN_PHASE_VARIANTS: List<String> = listOf(CONTEXT_SENSITIVE_VARIANT, NO_ALIAS_EXPANSION_VARIANT)

/**
 * [withTestDataVariants] except the variants whose tests check their own phase, e.g., `foo.noAliasExpansion.kt`: the variants a
 * `RUN_PIPELINE_TILL` edit of the test data goes to.
 */
val File.withPhaseSharingTestDataVariants: List<File>
    get() {
        val ownPhaseVariants = OWN_PHASE_VARIANTS.map { name.removeSuffix(extensionWithDot) + ".$it" + extensionWithDot }
        return withTestDataVariants.filter { it.name !in ownPhaseVariants }
    }

val File.isLatestLVTestData: Boolean
    get() = isCustomTestDataWithPrefix(LATEST_LV_PREFIX)

val File.isLfDisabledTestData: Boolean
    get() = isCustomTestDataWithPrefix(LF_DISABLED_PREFIX)

/**
 * @see File.llFirTestDataFile
 */
val File.isLLFirTestData: Boolean
    get() = isCustomTestDataWithPrefix(LL_FIR_PREFIX)

/**
 * Whether this is LL FIR test data diverging from the compiler's test data: `.ll.kt` or its Wasm variants, e.g., `.ll.wasmJs.kt`.
 */
val File.isLLFirDivergenceTestData: Boolean
    get() = LL_FIR_DIVERGENCE_VARIANTS.any { isCustomTestDataWithPrefix(".$it") }

val File.isCustomTestData: Boolean
    get() = isLLFirTestData || isLatestLVTestData || isLfDisabledTestData

private fun File.isCustomTestDataWithPrefix(prefix: String): Boolean = name.endsWith("$prefix$extensionWithDot")

val File.lfDisabledTestDataFile: File
    get() = getCustomTestDataFileWithPrefix(LF_DISABLED_PREFIX)

/**
 * An LL FIR test data file (`.ll.kt`) allows tailoring the expected output of a test to the LL FIR case. In very rare cases, LL FIR may
 * legally diverge from the output of the K2 compiler, such as when the compiler's error behavior is deliberately unspecified. (For an
 * example, see `kotlinJavaKotlinCycle.ll.kt`.)
 */
val File.llFirTestDataFile: File
    get() = getCustomTestDataFileWithPrefix(LL_FIR_PREFIX)

private fun File.getCustomTestDataFileWithPrefix(prefix: String): File =
    if (isCustomTestDataWithPrefix(prefix)) this
    else {
        // Because `File` can be `.ll.kt` or `.fir.kt` test data, we have to go off `originalTestDataFileName`, which removes the prefix
        // intelligently.
        val originalName = originalTestDataFileName
        val extension = extensionWithDot
        val customName = "${originalName.removeSuffix(extension)}$prefix$extension"
        parentFile.resolve(customName)
    }

val File.originalTestDataFile: File
    get() {
        val originalName = originalTestDataFileName
        return if (originalName != name) parentFile.resolve(originalName) else this
    }

val File.originalTestDataFileName: String
    get() {
        val prefix = when {
            isLLFirTestData -> LL_FIR_PREFIX
            isLatestLVTestData -> LATEST_LV_PREFIX
            isLfDisabledTestData -> LF_DISABLED_PREFIX
            else -> return name
        }
        return getOriginalTestDataFileNameFromPrefix(prefix)
    }

private fun File.getOriginalTestDataFileNameFromPrefix(prefix: String): String {
    val extension = extensionWithDot
    return "${name.removeSuffix("$prefix$extension")}$extension"
}

/**
 * The marker after the extension of test data which can freeze the IDE, e.g., `foo.kt.can-freeze-ide` (see `TestGeneratorUtil.canFreezeIDE`).
 */
private const val CAN_FREEZE_IDE_MARKER = ".can-freeze-ide"

/**
 * The extension of this test data with the dot, e.g., `.kt`, `.test.kts` for a script with a compound extension (see
 * [COMPOUND_SCRIPT_KINDS]), or `.kt.can-freeze-ide` with the [CAN_FREEZE_IDE_MARKER]. The variants of the test data go before it.
 */
val File.extensionWithDot: String
    get() {
        val nameWithoutMarker = name.removeSuffix(CAN_FREEZE_IDE_MARKER)
        val extension = COMPOUND_EXTENSIONS.firstOrNull { nameWithoutMarker.endsWith(it) }
            ?: ".${nameWithoutMarker.substringAfterLast('.', "")}"
        return extension + name.removePrefix(nameWithoutMarker)
    }
