/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.exceptions

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ExceptionWithAttachments
import org.jetbrains.kotlin.utils.exceptions.KotlinExceptionWithAttachments.Companion.withAttachmentsFrom
import java.nio.charset.StandardCharsets

interface KotlinExceptionWithAttachments : ExceptionWithAttachments {
    val mutableAttachments: MutableList<Attachment>

    override fun getAttachments(): Array<Attachment> = mutableAttachments.toTypedArray()

    fun withAttachment(name: String, content: Any?): KotlinExceptionWithAttachments {
        mutableAttachments.add(Attachment(name, content?.toString() ?: "<null>"))
        return this
    }

    companion object {
        internal fun KotlinExceptionWithAttachments.withAttachmentsFrom(from: Throwable?) {
            if (from is KotlinExceptionWithAttachments) {
                from.mutableAttachments.mapTo(mutableAttachments) { attachment ->
                    attachment.copyWithNewName("case_${attachment.path}")
                }
            }
            if (from != null) {
                withAttachment("causeThrowable", from.stackTraceToString())
            }
        }

        private fun Attachment.copyWithNewName(newName: String): Attachment {
            val content = String(bytes, StandardCharsets.UTF_8)
            return Attachment(newName, content)
        }
    }
}

open class KotlinIllegalStateExceptionWithAttachments : IllegalStateException, KotlinExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

open class KotlinRuntimeExceptionWithAttachments : RuntimeException, KotlinExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

open class KotlinIllegalArgumentExceptionWithAttachments : IllegalArgumentException, KotlinExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

