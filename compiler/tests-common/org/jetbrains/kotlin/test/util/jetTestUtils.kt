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

package org.jetbrains.kotlin.test.util

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartFMap
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import java.io.File

fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
        this.split('\n').map { it.trimEnd() }.joinToString(separator = "\n").let {
            result -> if (result.endsWith("\n")) result else result + "\n"
        }

fun PsiFile.findElementByCommentPrefix(commentText: String): PsiElement? =
        findElementsByCommentPrefix(commentText).keys.singleOrNull()

fun PsiFile.findElementsByCommentPrefix(prefix: String): Map<PsiElement, String> {
    var result = SmartFMap.emptyMap<PsiElement, String>()
    accept(
            object : KtTreeVisitorVoid() {
                override fun visitComment(comment: PsiComment) {
                    val commentText = comment.text
                    if (commentText.startsWith(prefix)) {
                        val parent = comment.parent
                        val elementToAdd = when (parent) {
                            is KtDeclaration -> parent
                            is PsiMember -> parent
                            else -> PsiTreeUtil.skipSiblingsForward(
                                    comment,
                                    PsiWhiteSpace::class.java, PsiComment::class.java, KtPackageDirective::class.java
                            )
                        } as? PsiElement ?: return

                        result = result.plus(elementToAdd, commentText.substring(prefix.length).trim())
                    }
                }
            }
    )
    return result
}

fun lastModificationDate(dir: File): Long {
    var lastModified: Long = -1L

    FileUtil.processFilesRecursively(dir) { file ->
        val fileModified = file.lastModified()
        if (fileModified > lastModified) {
            lastModified = fileModified
        }

        true
    }

    return lastModified
}