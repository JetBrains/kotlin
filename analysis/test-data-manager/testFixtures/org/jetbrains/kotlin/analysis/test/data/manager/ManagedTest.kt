/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import java.nio.file.Path

/**
 * Chain of test variant identifiers that determines output file naming and execution priority.
 *
 * Each element represents a level of specialization:
 * - `[]` — golden/default configuration, writes to `.txt`
 * - `["standalone"]` — single variant, writes to `.standalone.txt`
 * - `["knm", "wasm"]` — nested variant, writes to `.wasm.txt` (last element)
 *
 * **Priority:** Shorter chains run first (golden → single-level → multi-level).
 *
 * **File resolution:** Tests can read from any file in their chain + the golden one
 * (e.g., `["knm", "wasm"]` can read `.txt`, `.knm.txt`, `.wasm.txt`).
 *
 * This means:
 * - Tests with `["knm", "wasm"]` can inherit expected output from `.knm.txt` if no `.wasm.txt` exists. `.txt` (golden) will be used only if both `.knm.txt` and `.wasm.txt` don't exist.
 * - Tests with just `["standalone"]` can only inherit from `.txt` (golden).
 *
 * **Output:** Only the last element determines the output file suffix.
 *
 * **Composing chains:** Use [withAdditionalVariant] to extend a base chain with an extra specialization dimension
 * (e.g., adding `"dangling"` to differentiate dangling-file tests that share the same test data directory).
 *
 * See [README.md](https://github.com/JetBrains/kotlin/blob/master/analysis/test-data-manager/README.md) for more details.
 *
 * @see ManagedTest.variantChain
 * @see withAdditionalVariant
 */
typealias TestVariantChain = List<String>

/**
 * Creates a refined variant chain by adding [variant] as an additional specialization.
 *
 * The resulting chain includes:
 * 1. The original chain elements (for fallback to base variant files)
 * 2. The [variant] alone (as a cross-variant fallback)
 * 3. Each original element combined with [variant] (most specific)
 *
 * Example: `["standalone.fir"].withAdditionalVariant("dangling")`
 *   → `["standalone.fir", "dangling", "standalone.fir.dangling"]`
 *
 * Read priority (reversed): `.standalone.fir.dangling.txt` → `.dangling.txt` → `.standalone.fir.txt` → `.txt`
 * Write target: `.standalone.fir.dangling.txt`
 */
fun TestVariantChain.withAdditionalVariant(variant: String): TestVariantChain = buildList {
    addAll(this@withAdditionalVariant)         // original chain for fallback
    add(variant)                               // standalone new variant
    for (existing in this@withAdditionalVariant) {
        add("$existing.$variant")              // combined: existing.new
    }
}

/**
 * Represents a test that can be processed by the Managed Test Data system.
 *
 * Use [assertEqualsToTestDataFile] extension function for comparing test outputs with expected files.
 *
 * See [README.md](https://github.com/JetBrains/kotlin/blob/master/analysis/test-data-manager/README.md) for system architecture and requirements.
 *
 * @see assertEqualsToTestDataFile
 * @see ManagedTestAssertions.assertEqualsToTestDataFile
 */
interface ManagedTest {
    /**
     * Chain of test variant identifiers that determines output file naming and execution priority.
     *
     * @see assertEqualsToTestDataFile
     * @see ManagedTestAssertions.assertEqualsToTestDataFile
     */
    val variantChain: TestVariantChain get() = emptyList()
}

/**
 * Extension for convenient usage from [ManagedTest] implementations.
 */
fun ManagedTest.assertEqualsToTestDataFile(
    testDataPath: Path,
    actual: String?,
    extension: String,
) {
    ManagedTestAssertions.assertEqualsToTestDataFile(
        testDataPath = testDataPath,
        actual = actual,
        variantChain = variantChain,
        extension = extension,
    )
}
