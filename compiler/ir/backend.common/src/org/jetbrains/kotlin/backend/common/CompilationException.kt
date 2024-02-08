/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.fileOrNull

/**
 * @param file If [file] is not known at this moment, [initializeFileDetails] should be invoked later in a `catch` to initialize the
 * exception's file details.
 */
class CompilationException(
    message: String,
    file: IrFile?,
    ir: Any?, /* IrElement | IrType */
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    /**
     * [CompilationException] must not keep strong references to IR entities. This is because, in the IDE, the exception might be kept in a
     * message log, which can prevent the IR entity and all its references (including FIR and PSI elements) from being garbage-collected.
     *
     * Hence, [CompilationExceptionFileDetails] encapsulates the detailed information gathered from the [IrFile], without keeping a
     * reference to it. If the exception is never initialized with an [IrFile], the file details won't be included in the message.
     */
    private var fileDetails: CompilationExceptionFileDetails? = null

    val irStartOffset: Int? = (ir as? IrElement)?.startOffset

    val content: String? = when (ir) {
        is IrElement -> ir.dumpKotlinLike()
        is IrType -> ir.dumpKotlinLike()
        else -> null
    }

    init {
        file?.let(::initializeFileDetails)
    }

    fun initializeFileDetails(file: IrFile) {
        if (fileDetails != null) return

        fileDetails = CompilationExceptionFileDetails(file, irStartOffset)
    }

    val path: String? get() = fileDetails?.path

    val line: Int get() = fileDetails?.line ?: UNDEFINED_OFFSET

    val column: Int get() = fileDetails?.column ?: UNDEFINED_OFFSET

    override val message: String
        get() = try {
            buildString {
                appendLine("Back-end: Please report this problem https://kotl.in/issue")
                fileDetails?.render(this)
                content?.let { appendLine("Problem with `$it`") }
                append("Details: " + super.message)
            }
        } catch (_: Throwable) {
            throw IllegalStateException("Problem with constructing exception message").also {
                it.stackTrace = stackTrace
            }
        }
}

/**
 * @see CompilationException.fileDetails
 */
private class CompilationExceptionFileDetails(file: IrFile, irStartOffset: Int?) {
    val path: String = file.path

    val line: Int = run {
        if (irStartOffset == null || irStartOffset == UNDEFINED_OFFSET) {
            return@run UNDEFINED_OFFSET
        }
        file.fileEntry.getLineNumber(irStartOffset) + 1
    }

    val column: Int = run {
        if (irStartOffset == null || irStartOffset == UNDEFINED_OFFSET) {
            return@run UNDEFINED_OFFSET
        }
        file.fileEntry.getColumnNumber(irStartOffset) + 1
    }

    fun render(stringBuilder: StringBuilder) {
        stringBuilder.appendLine("$path:$line:$column")
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
    } catch (_: Throwable) {
        null
    }
    throw CompilationException(message, file, declaration)
}

fun Throwable.wrapWithCompilationException(
    message: String,
    file: IrFile,
    element: IrElement?
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