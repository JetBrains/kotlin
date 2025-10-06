/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.util.SourceCodeAnalysisException
import org.jetbrains.kotlin.util.getExceptionMessage
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
        ): Nothing {
            // CompilationException (the only KotlinExceptionWithAttachments possible here) is already supposed
            // to have all information about the context.
            if (exception is KotlinExceptionWithAttachments) throw exception
            rethrowIntellijPlatformExceptionIfNeeded(exception)
            val locationWithLineAndOffset = location
                ?.let { exception as? SourceCodeAnalysisException }
                ?.let { linesMapping(it.source.startOffset) }
                ?.let { (line, offset) -> "$location:${line + 1}:${offset + 1}" }
                ?: location
            throw BackendException(
                getExceptionMessage("Backend", "Exception during $phase", exception, locationWithLineAndOffset) +
                        additionalMessage?.let { "\n" + it }.orEmpty(),
                exception
            )
        }
    }
}