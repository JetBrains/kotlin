/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.SourceFileProvider
import org.jetbrains.kotlin.test.testInfraError

/**
 * Selects the source representation used by source-inspection helpers.
 *
 * [ORIGINAL] is the only view available while module structure extraction is still producing additional files.
 * [TRANSFORMED] is the compiler-input view and must be used when an inspection is meant to describe an already
 * configured compilation.
 */
enum class SourceContentView {
    ORIGINAL,
    TRANSFORMED,
}

/** Returns the selected source representation, failing rather than silently falling back when it is unavailable. */
fun getSourceContent(
    file: TestFile,
    sourceContentView: SourceContentView,
    sourceFileProvider: SourceFileProvider?,
): String = when (sourceContentView) {
    SourceContentView.ORIGINAL -> file.originalContent
    SourceContentView.TRANSFORMED -> sourceFileProvider?.getContentOfSourceFile(file) ?: testInfraError(
        "The transformed source view for '${file.relativePath}' requires a registered SourceFileProvider"
    )
}

internal fun sourceContentViewFor(sourceFileProvider: SourceFileProvider?): SourceContentView =
    if (sourceFileProvider == null) SourceContentView.ORIGINAL else SourceContentView.TRANSFORMED
