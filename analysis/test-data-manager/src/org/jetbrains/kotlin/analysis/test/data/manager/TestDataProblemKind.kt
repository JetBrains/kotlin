/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

/** How a test data file doesn't match what its test produced. */
enum class TestDataProblemKind {
    /** The file exists, but its content differs from the actual output. */
    MISMATCH,

    /** No file in the variant chain exists; the file is the one to create. */
    MISSING_FILE,

    /** The file has the same content as the next less specific file in the variant chain and should be deleted. */
    REDUNDANT_FILE,

    /** The file exists while the test produced no output. */
    UNEXPECTED_FILE,
}
