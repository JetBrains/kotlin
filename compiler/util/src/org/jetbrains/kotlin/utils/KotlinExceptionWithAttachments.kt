/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.utils.exceptions.KotlinExceptionWithAttachments as KotlinExceptionWithAttachmentsBase
import org.jetbrains.kotlin.utils.exceptions.KotlinExceptionWithAttachments.Companion.withAttachmentsFrom
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class KotlinExceptionWithAttachments : RuntimeException, KotlinExceptionWithAttachmentsBase {
    override val mutableAttachments = mutableListOf<Attachment>()

    override fun withAttachment(name: String, content: Any?): KotlinExceptionWithAttachments {
        return super.withAttachment(name, content) as KotlinExceptionWithAttachments
    }

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }

    fun withPsiAttachment(name: String, element: PsiElement?): KotlinExceptionWithAttachments {
        kotlin.runCatching { ApplicationManager.getApplication().runReadAction<String> { element?.let(::getElementTextWithContext) } }
            .getOrNull()?.let { withAttachment(name, it) }
        return this
    }
}


@OptIn(ExperimentalContracts::class)
inline fun checkWithAttachment(value: Boolean, lazyMessage: () -> String, attachments: (org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments) -> Unit = {}) {
    contract { returns() implies (value) }

    if (!value) {
        val e = KotlinExceptionWithAttachments(lazyMessage())
        attachments(e)
        throw e
    }
}