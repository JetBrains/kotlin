/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.BirFile
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.ancestors
import org.jetbrains.kotlin.bir.util.path
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class CompilationException(
    message: String,
    val file: BirFile?,
    val bir: Any?, /* BirElement | BirType */
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    override val message: String
        get() = try {
            buildString {
                appendLine("Back-end: Please report this problem https://kotl.in/issue")
                path?.let { appendLine("$it:$line:$column") }
                content?.let { appendLine("Problem with `$it`") }
                append("Details: " + super.message)
            }
        } catch (e: Throwable) {
            throw IllegalStateException("Problem with constructing exception message").also {
                it.stackTrace = stackTrace
            }
        }

    val line: Int
        get() {
            val birSourceSpan = birSourceSpan
                ?: return UNDEFINED_OFFSET

            if (birSourceSpan.isUndefined) return UNDEFINED_OFFSET

            // todo
            return UNDEFINED_OFFSET
            /*val lineNumber = file?.fileEntry?.getLineNumber(birSourceSpan.start)
                ?: return UNDEFINED_OFFSET

            return lineNumber + 1*/
        }

    val column: Int
        get() {
            val birSourceSpan = birSourceSpan
                ?: return UNDEFINED_OFFSET

            if (birSourceSpan.isUndefined) return UNDEFINED_OFFSET

            // todo
            return UNDEFINED_OFFSET
            /*val columnNumber = file?.fileEntry?.getColumnNumber(birSourceSpan.start)
                ?: return UNDEFINED_OFFSET

            return columnNumber + 1*/
        }

    private val birSourceSpan: SourceSpan?
        get() = (bir as? BirElement)?.sourceSpan

    val path: String?
        get() = file?.path

    val content: String?
        get() = when (bir) {
            is BirElement -> bir.render() // bir.dumpKotlinLike()
            is BirType -> bir.render() // bir.dumpKotlinLike()
            else -> null
        }
}

fun compilationException(message: String, element: BirElement): Nothing {
    val file = try {
        element.ancestors().firstIsInstanceOrNull<BirFile>()
    } catch (e: Throwable) {
        null
    }
    throw CompilationException(message, file, element)
}

fun compilationException(message: String, type: BirType?): Nothing {
    throw CompilationException(message, null, type)
}


fun Throwable.wrapWithCompilationException(
    message: String,
    file: BirFile,
    element: BirElement?
): RuntimeException {
    return if (this is ProcessCanceledException)
        this
    else
        CompilationException(
            "$message: ${this::class.qualifiedName}: ${this.message}",
            file,
            element,
            cause = this
        ).apply {
            stackTrace = this@wrapWithCompilationException.stackTrace
        }
}