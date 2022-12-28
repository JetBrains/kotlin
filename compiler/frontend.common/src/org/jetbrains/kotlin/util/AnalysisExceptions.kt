/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.project.IndexNotReadyException
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement

val Throwable.classNameAndMessage get() = "${this::class.qualifiedName}: $message"

class SourceCodeAnalysisException(val source: KtSourceElement, override val cause: Throwable) : Exception() {
    override val message get() = cause.classNameAndMessage
}


fun Throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element: KtSourceElement?) = when (this) {
    is SourceCodeAnalysisException -> this
    is IndexNotReadyException -> this
    is ControlFlowException -> this
    is VirtualMachineError -> this
    else -> when (element?.kind) {
        is KtRealSourceElementKind -> SourceCodeAnalysisException(element, this)
        else -> this
    }
}

class FileAnalysisException(
    private val path: String,
    override val cause: Throwable,
    private val lineAndOffset: Pair<Int, Int>? = null,
) : Exception() {
    override val message
        get(): String {
            val (line, offset) = lineAndOffset ?: return "Somewhere in file $path: ${cause.classNameAndMessage}"
            return "While analysing $path:${line + 1}:${offset + 1}: ${cause.classNameAndMessage}"
        }
}

fun Throwable.wrapIntoFileAnalysisExceptionIfNeeded(
    filePath: String?,
    fileSource: AbstractKtSourceElement?,
    linesMapping: (Int) -> Pair<Int, Int>?,
) = when {
    filePath == null -> this

    this is SourceCodeAnalysisException -> when (fileSource) {
        source -> FileAnalysisException(filePath, cause)
        else -> FileAnalysisException(filePath, cause, linesMapping(source.startOffset))
    }

    this is IndexNotReadyException -> this
    this is ControlFlowException -> this
    this is VirtualMachineError -> this
    else -> FileAnalysisException(filePath, this)
}

inline fun <R> withSourceCodeAnalysisExceptionUnwrapping(block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        throw if (throwable is SourceCodeAnalysisException) throwable.cause else throwable
    }
}
