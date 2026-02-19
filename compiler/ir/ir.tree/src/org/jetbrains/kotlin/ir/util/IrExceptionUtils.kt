/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder

class IrExceptionBuilder(val message: String) {
    private val attachmentBuilder = ExceptionAttachmentBuilder()

    fun withIrEntry(name: String, ir: IrElement) = attachmentBuilder.withEntry(name, ir) {
        ir.render()
    }

    fun buildString(): String = buildString {
        appendLine(message)
        append(attachmentBuilder.buildString())
    }
}

inline fun irError(
    message: String,
    buildAttachment: IrExceptionBuilder.() -> Unit = {},
): Nothing {
    val builder = IrExceptionBuilder(message).apply { buildAttachment() }
    error(builder.buildString())
}