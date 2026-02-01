/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

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
 * See [README.md](https://github.com/JetBrains/kotlin/blob/master/analysis/test-data-manager/README.md) for more details.
 *
 * @see ManagedTest.variantChain
 */
typealias TestVariantChain = List<String>

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
