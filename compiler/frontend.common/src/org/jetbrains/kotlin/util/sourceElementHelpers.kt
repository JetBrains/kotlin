/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement

/**
 * Converts the [KtSourceElement] to a string describing its location.
 *
 * The function should not be used in production due to its reliance on file text processing. It is intended for usage in tests and
 * assertion messages. It may be temporarily used from a production location (e.g., via a flag), hence its placement in production sources.
 */
@TestOnly
fun KtSourceElement?.toDebugLocationDescription(): String =
    when (this) {
        null -> "<unknown location: no source element>"

        is KtPsiSourceElement -> {
            val pos = StringUtil.offsetToLineColumn(psi.containingFile.text, startOffset)
            val lineColumn = "${pos.line + 1}:${pos.column + 1}"

            buildString {
                append(this@toDebugLocationDescription)
                append(" on line ")
                append(lineColumn)
            }
        }

        else -> toString()
    }
