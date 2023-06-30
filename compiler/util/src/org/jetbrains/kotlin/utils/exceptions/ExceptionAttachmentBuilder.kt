/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.exceptions

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class ExceptionAttachmentBuilder {
    private val printer = SmartPrinter(StringBuilder())

    fun <T> withEntry(name: String, value: T, render: (T & Any) -> String) {
        withEntry(name) {
            println("Class: ${value?.let { it::class.java.name } ?: "<null>"}")
            println("Value:")
            withIndent {
                println(value?.let(render) ?: "<null>")
            }
        }
    }

    fun withEntry(name: String, value: String?) {
        with(printer) {
            println("- $name:")
            withIndent {
                println(value ?: "<null>")
            }
            println(separator)
        }
    }

    fun withEntry(name: String, buildValue: SmartPrinter.() -> Unit) {
        withEntry(name, SmartPrinter(StringBuilder()).apply(buildValue).toString())
    }

    fun withEntryGroup(groupName: String, build: ExceptionAttachmentBuilder.() -> Unit) {
        val builder = ExceptionAttachmentBuilder().apply(build)
        withEntry(groupName, builder) { it.buildString() }
    }

    fun buildString(): String = printer.toString()

    private companion object {
        private const val separator = "========"
    }
}

inline fun KotlinExceptionWithAttachments.buildAttachment(
    name: String = "info.txt",
    buildContent: ExceptionAttachmentBuilder.() -> Unit,
): KotlinExceptionWithAttachments {
    return withAttachment(name, ExceptionAttachmentBuilder().apply(buildContent).buildString())
}

inline fun buildErrorWithAttachment(
    message: String,
    cause: Exception? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    val exception = KotlinIllegalArgumentExceptionWithAttachments(message, cause)
    exception.buildAttachment(attachmentName) { buildAttachment() }
    throw exception
}

inline fun Logger.logErrorWithAttachment(
    message: String,
    cause: Exception? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    val attachment = Attachment(attachmentName, ExceptionAttachmentBuilder().apply(buildAttachment).buildString())
    this.error(message, cause, attachment)
}

inline fun rethrowExceptionWithDetails(
    message: String,
    exception: Exception,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    if (shouldIjPlatformExceptionBeRethrown(exception)) throw exception
    buildErrorWithAttachment(message, exception, attachmentName, buildAttachment)
}


@OptIn(ExperimentalContracts::class)
inline fun checkWithAttachmentBuilder(
    condition: Boolean,
    message: () -> String,
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        buildErrorWithAttachment(message(), buildAttachment = buildAttachment)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun requireWithAttachmentBuilder(
    condition: Boolean,
    message: () -> String,
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        buildErrorWithAttachment(message(), buildAttachment = buildAttachment)
    }
}
