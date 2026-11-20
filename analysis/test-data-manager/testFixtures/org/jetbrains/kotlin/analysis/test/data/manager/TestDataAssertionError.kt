/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.nio.file.Path

/**
 * A managed test data file that doesn't match the test output, thrown by [ManagedTestAssertions].
 *
 * Unlike other failures, it is a test data problem that `updateTestData` resolves by writing or deleting [file].
 */
internal class TestDataAssertionError : AssertionFailedError {
    /** What is wrong with [file]. */
    val kind: Kind

    /** The file `updateTestData` would create, update or delete. */
    val file: Path

    /**
     * The existing file the output was compared with: [file] itself or a less specific file of the variant chain
     * for [Kind.MISMATCH], the less specific file with the same content for [Kind.REDUNDANT_FILE], otherwise `null`.
     */
    val comparedFile: Path?

    /**
     * A failure with the [expected] file and the [actual] content, so IDEs can show the comparison.
     */
    constructor(
        message: String,
        kind: Kind,
        file: Path,
        comparedFile: Path?,
        expected: FileInfo,
        actual: String,
    ) : super(message, expected, actual) {
        this.kind = kind
        this.file = file
        this.comparedFile = comparedFile
    }

    /**
     * A failure without a comparison: passing `null` as the expected value would still define one.
     */
    constructor(message: String, kind: Kind, file: Path, comparedFile: Path?) : super(message) {
        this.kind = kind
        this.file = file
        this.comparedFile = comparedFile
    }

    enum class Kind {
        /** [file] exists, but its content differs from the actual output. */
        MISMATCH,

        /** No file in the variant chain exists; [file] is the one to create. */
        MISSING_FILE,

        /** [file] has the same content as the next less specific file in the variant chain and should be deleted. */
        REDUNDANT_FILE,

        /** [file] exists while the test produced no output. */
        UNEXPECTED_FILE,
    }
}
