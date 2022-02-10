/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fileOrNull

class CompilationException(
    message: String,
    // file is not known in any moment, need to set it later in catch to save stacktrace
    var file: IrFile?,
    val ir: Any?, /* IrElement | IrType */
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    override val message: String
        get() = try {
            buildString {
                appendLine("Back-end: Please report this problem https://kotl.in/issue")
                path?.let { appendLine("$it:$line:$column") }
                content?.let { appendLine("Problem with `$it`") }
                append("File: ${file?.path}")
                append("Details: " + super.message)
            }
        } catch (e: Throwable) {
            throw IllegalStateException("Problem with constructing exception message").also {
                it.stackTrace = stackTrace
            }
        }

    val line: Int
        get() {
            val irStartOffset = irStartOffset
                ?: return UNDEFINED_OFFSET

            if (irStartOffset == UNDEFINED_OFFSET) return UNDEFINED_OFFSET

            val lineNumber = file?.fileEntry?.getLineNumber(irStartOffset)
                ?: return UNDEFINED_OFFSET

            return lineNumber + 1
        }

    val column: Int
        get() {
            val irStartOffset = irStartOffset
                ?: return UNDEFINED_OFFSET

            if (irStartOffset == UNDEFINED_OFFSET) return UNDEFINED_OFFSET

            val columnNumber = file?.fileEntry?.getColumnNumber(irStartOffset)
                ?: return UNDEFINED_OFFSET

            return columnNumber + 1
        }

    private val irStartOffset: Int?
        get() = (ir as? IrElement)?.startOffset

    val path: String?
        get() = file?.path

    val content: String?
        get() = when (ir) {
            is IrElement -> ir.dumpKotlinLike()
            is IrType -> ir.dumpKotlinLike()
            else -> null
        }
}

fun compilationException(message: String, element: IrElement): Nothing {
    throw CompilationException(message, null, element)
}

fun compilationException(message: String, type: IrType?): Nothing {
    throw CompilationException(message, null, type)
}

fun compilationException(message: String, declaration: IrDeclaration): Nothing {
    val file = try {
        declaration.fileOrNull
    } catch (e: Throwable) {
        null
    }
    throw CompilationException(message, file, declaration)
}

fun Throwable.wrapWithCompilationException(
    message: String,
    file: IrFile,
    element: IrElement?
): CompilationException {
    return CompilationException(
        "$message: ${this::class.qualifiedName}: ${this.message}",
        file,
        element,
        cause = this
    ).apply {
        stackTrace = this@wrapWithCompilationException.stackTrace
    }
}