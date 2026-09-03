/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.exceptions

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.utils.getElementTextWithContext

fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?) {
    withEntry(name, psi) { psiElement ->
        getElementTextWithContext(psiElement)
    }
}

fun ExceptionAttachmentBuilder.withVirtualFileEntry(name: String, virtualFile: VirtualFile?) {
    withEntry(name, virtualFile) { file ->
        "path: ${file.path}, filetype: ${file.fileType} ,filesystem,${file.fileSystem}"
    }
}