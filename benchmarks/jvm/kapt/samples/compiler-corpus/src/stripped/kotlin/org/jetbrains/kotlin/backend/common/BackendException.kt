/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.util.SourceCodeAnalysisException
import org.jetbrains.kotlin.utils.exceptions.getExceptionMessage
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded

class BackendException(message: String, cause: Throwable?) : IllegalStateException(message, cause) {
    companion object {
        fun report(
            exception: Throwable,
            phase: String,
            location: String?,
            additionalMessage: String? = null,
            linesMapping: (Int) -> Pair<Int, Int>? = { _ -> null },
        ): Nothing { TODO() }
    }
}
