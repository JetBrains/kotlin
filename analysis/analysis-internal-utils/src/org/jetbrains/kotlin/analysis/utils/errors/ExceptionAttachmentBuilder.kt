/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.errors

import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

public class ExceptionAttachmentBuilder {
    private val printer = PrettyPrinter()

    public fun <T> withEntry(name: String, value: T, render: (T & Any) -> String) {
        withEntry(name) {
            appendLine("Class: ${value?.let { it::class.java.name } ?: "<null>"}")
            appendLine("Value:")
            withIndent {
                appendLine(value?.let(render) ?: "<null>")
            }
        }
    }

    public fun withEntry(name: String, value: String?) {
        with(printer) {
            appendLine("- $name:")
            withIndent {
                appendLine(value ?: "<null>")
            }
            appendLine(separator)
        }
    }

    public fun withEntry(name: String, buildValue: PrettyPrinter.() -> Unit) {
        withEntry(name, prettyPrint { buildValue() })
    }

    public fun withEntryGroup(groupName: String, build: ExceptionAttachmentBuilder.() -> Unit) {
        val builder = ExceptionAttachmentBuilder().apply(build)
        withEntry(groupName, builder) { it.buildString() }
    }

    public fun buildString(): String = printer.toString()

    private companion object {
        private const val separator = "========"
    }
}

public inline fun KotlinExceptionWithAttachments.buildAttachment(
    name: String = "info.txt",
    buildContent: ExceptionAttachmentBuilder.() -> Unit
): KotlinExceptionWithAttachments {
    return withAttachment(name, ExceptionAttachmentBuilder().apply(buildContent).buildString())
}

public inline fun buildErrorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {}
): Nothing {
    val exception = KotlinExceptionWithAttachments(message, cause)
    exception.buildAttachment(attachmentName) { buildAttachment() }
    throw exception
}


@OptIn(ExperimentalContracts::class)
public inline fun checkWithAttachmentBuilder(
    condition: Boolean,
    message: () -> String,
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {}
) {
    contract { returns() implies (condition) }

    if (!condition) {
        buildErrorWithAttachment(message(), buildAttachment = buildAttachment)
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun requireWithAttachmentBuilder(
    condition: Boolean,
    message: () -> String,
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {}
) {
    contract { returns() implies (condition) }

    if (!condition) {
        buildErrorWithAttachment(message(), buildAttachment = buildAttachment)
    }
}
