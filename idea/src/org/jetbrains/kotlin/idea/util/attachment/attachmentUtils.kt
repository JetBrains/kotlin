/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.util.attachment

import com.intellij.psi.PsiFile
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.diagnostic.AttachmentFactory

public fun attachmentByPsiFileAsArray(file: PsiFile?): Array<Attachment> {
    val attachment = attachmentByPsiFile(file)
    if (attachment == null) {
        return arrayOf()
    }
    return arrayOf(attachment)
}

public fun attachmentByPsiFile(file: PsiFile?): Attachment? {
    if (file == null) return null

    val virtualFile = file.getVirtualFile()
    if (virtualFile != null) return AttachmentFactory.createAttachment(virtualFile)

    val text = try { file.getText() } catch(e: Exception) { null }
    val name = try { file.getName() } catch(e: Exception) { null }

    if (text != null && name != null) return Attachment(name, text)

    return null
}

public fun mergeAttachments(vararg attachments: Attachment?): Attachment {
    val builder = StringBuilder()
    attachments.forEach {
        if (it != null) {
            builder.append("\n----- START ${it.getPath()} -----\n")
            builder.append(it.getDisplayText())
            builder.append("\n----- END   ${it.getPath()} -----\n")
        }
    }

    return Attachment("message.txt", builder.toString())
}
