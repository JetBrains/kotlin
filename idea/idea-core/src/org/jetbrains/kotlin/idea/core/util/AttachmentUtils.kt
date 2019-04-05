/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.util

import com.intellij.diagnostic.AttachmentFactory
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.psi.PsiFile

fun attachmentByPsiFile(file: PsiFile?): Attachment? {
    if (file == null) return null

    val virtualFile = file.virtualFile
    if (virtualFile != null) return AttachmentFactory.createAttachment(virtualFile)

    val text = try { file.text
    } catch(e: Exception) { null }
    val name = try { file.name
    } catch(e: Exception) { null }

    if (text != null && name != null) return Attachment(name, text)

    return null
}

fun mergeAttachments(vararg attachments: Attachment?): Attachment {
    val builder = StringBuilder()
    attachments.forEach {
        if (it != null) {
            builder.append("----- START ${it.path} -----\n")
            builder.append(it.displayText)
            builder.append("\n----- END ${it.path} -----\n\n")
        }
    }

    return Attachment("message.txt", builder.toString())
}
