/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.exceptions

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.powerassert.ExperimentalPowerAssert
import kotlin.powerassert.PowerAssert
import kotlin.powerassert.toDefaultMessage

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


@OptIn(ExperimentalContracts::class, ExperimentalPowerAssert::class)
@PowerAssert
inline fun checkWithAttachment(
    condition: Boolean,
    @PowerAssert.Ignore message: () -> String,
    @PowerAssert.Ignore attachmentName: String = "info.txt",
    @PowerAssert.Ignore buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        val exception = KotlinIllegalStateExceptionWithAttachments(message())
        exception.buildAttachment(attachmentName) {
            buildAttachment()

            val explanation = PowerAssert.explanation
            if (explanation != null) {
                withEntry("explanation", explanation.toDefaultMessage())
            }
        }
        throw exception
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalPowerAssert::class)
@PowerAssert
inline fun requireWithAttachment(
    condition: Boolean,
    @PowerAssert.Ignore message: () -> String,
    @PowerAssert.Ignore attachmentName: String = "info.txt",
    @PowerAssert.Ignore buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        val exception = KotlinIllegalArgumentExceptionWithAttachments(message())
        exception.buildAttachment(attachmentName) {
            buildAttachment()

            val explanation = PowerAssert.explanation
            if (explanation != null) {
                withEntry("explanation", explanation.toDefaultMessage())
            }
        }
        throw exception
    }
}
