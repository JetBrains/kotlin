/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ExceptionWithAttachments
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class KotlinExceptionWithAttachments : RuntimeException, ExceptionWithAttachments {
    private val attachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        if (cause is KotlinExceptionWithAttachments) {
            attachments.addAll(cause.attachments)
        }
    }

    override fun getAttachments(): Array<Attachment> = attachments.toTypedArray()

    fun withAttachment(name: String, content: Any?): KotlinExceptionWithAttachments {
        attachments.add(Attachment(name, content?.toString() ?: "<null>"))
        return this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun checkWithAttachment(value: Boolean, lazyMessage: () -> String, attachments: (KotlinExceptionWithAttachments) -> Unit = {}) {
    contract { returns() implies(value) }

    if (!value) {
        val e = KotlinExceptionWithAttachments(lazyMessage())
        attachments(e)
        throw e
    }
}
