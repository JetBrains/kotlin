/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.exceptions

import com.intellij.openapi.diagnostic.Logger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class ExceptionAttachmentBuilder {
    private val builder = StringBuilder()

    fun <T> withEntry(name: String, value: T, render: (T & Any) -> String) {
        withEntry(name) {
            appendLine("Class: ${value?.let { it::class.java.name } ?: "<null>"}")
            appendLine("Value:")
            appendIndented(value?.let(render) ?: "<null>")
        }
    }

    fun withEntry(name: String, value: String?) {
        with(builder) {
            append("- ").append(name).appendLine(":")
            appendIndented(value ?: "<null>")
            appendLine(separator)
        }
    }

    fun withEntry(name: String, buildValue: StringBuilder.() -> Unit) {
        withEntry(name, StringBuilder().apply(buildValue).toString())
    }

    fun withEntryGroup(groupName: String, build: ExceptionAttachmentBuilder.() -> Unit) {
        val builder = ExceptionAttachmentBuilder().apply(build)
        withEntry(groupName, builder) { it.buildString() }
    }

    fun buildString(): String = builder.toString()

    private companion object {
        private const val separator = "========"
        private const val indent = "    "

        private fun StringBuilder.appendIndented(value: String) {
            for (line in value.lines()) {
                if (line.isBlank()) appendLine() else append(indent).appendLine(line)
            }
        }
    }
}

inline fun KotlinExceptionWithAttachments.buildAttachment(
    name: String = "info.txt",
    buildContent: ExceptionAttachmentBuilder.() -> Unit,
): KotlinExceptionWithAttachments {
    return withAttachment(name, ExceptionAttachmentBuilder().apply(buildContent).buildString())
}


inline fun Logger.logErrorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    this.error(buildErrorWithAttachment(message, cause, attachmentName, buildAttachment))
}

inline fun buildErrorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Throwable {
    val exception = KotlinIllegalArgumentExceptionWithAttachments(message, cause)
    exception.buildAttachment(attachmentName) { buildAttachment() }
    return exception
}

inline fun errorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    throw buildErrorWithAttachment(message, cause, attachmentName, buildAttachment)
}

inline fun rethrowExceptionWithDetails(
    message: String,
    exception: Throwable,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    rethrowIntellijPlatformExceptionIfNeeded(exception)
    errorWithAttachment(message, exception, attachmentName, buildAttachment)
}


@OptIn(ExperimentalContracts::class)
inline fun checkWithAttachment(
    condition: Boolean,
    message: () -> String,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        val exception = KotlinIllegalStateExceptionWithAttachments(message())
        exception.buildAttachment(attachmentName) { buildAttachment() }
        throw exception
    }
}

@OptIn(ExperimentalContracts::class)
inline fun requireWithAttachment(
    condition: Boolean,
    message: () -> String,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        val exception = KotlinIllegalArgumentExceptionWithAttachments(message())
        exception.buildAttachment(attachmentName) { buildAttachment() }
        throw exception
    }
}
