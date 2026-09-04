/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.SourceFileProvider
import org.jetbrains.kotlin.test.testInfraError

enum class SourceContentView {
    ORIGINAL,
    TRANSFORMED,
}

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
