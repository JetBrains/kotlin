/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.progress.ProcessCanceledException

val Throwable.classNameAndMessage get() = "${this::class.qualifiedName}: $message"

class SourceCodeAnalysisException(val source: KtSourceElement, override val cause: Throwable) : Exception() {
    override val message get() = cause.classNameAndMessage
}

inline fun <R> whileAnalysing(element: KtSourceElement?, block: () -> R): R {
    return try {
        block()
    } catch (exception: SourceCodeAnalysisException) {
        throw exception
    } catch (exception: ProcessCanceledException) {
        throw exception
    } catch (exception: Exception) {
        val source = element?.takeIf { it is KtRealPsiSourceElement } ?: throw exception
        throw SourceCodeAnalysisException(source, exception)
    } catch (error: StackOverflowError) {
        val source = element?.takeIf { it is KtRealPsiSourceElement } ?: throw error
        throw SourceCodeAnalysisException(source, error)
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

inline fun <R> withFileAnalysisExceptionWrapping(
    filePath: String?,
    fileSource: AbstractKtSourceElement?,
    linesMapping: (Int) -> Pair<Int, Int>?,
    block: () -> R,
): R {
    return try {
        block()
    } catch (exception: SourceCodeAnalysisException) {
        val path = filePath ?: throw exception

        if (fileSource == exception.source) {
            throw FileAnalysisException(path, exception.cause)
        }

        val lineAndOffset = linesMapping(exception.source.startOffset)
        throw FileAnalysisException(path, exception.cause, lineAndOffset)
    } catch (exception: Exception) {
        val path = filePath ?: throw exception
        throw FileAnalysisException(path, exception)
    } catch (error: StackOverflowError) {
        val path = filePath ?: throw error
        throw FileAnalysisException(path, error)
    }
}
