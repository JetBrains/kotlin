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

import com.intellij.codeInspection.SmartHashMap
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartFMap
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.util.*

public fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
        this.split('\n').map { it.trimEnd() }.joinToString(separator = "\n").let {
            result -> if (result.endsWith("\n")) result else result + "\n"
        }

public fun CodeInsightTestFixture.configureWithExtraFile(path: String, vararg extraNameParts: String) {
    configureWithExtraFile(path, *extraNameParts)
}

public fun CodeInsightTestFixture.configureWithExtraFile(path: String, vararg extraNameParts: String = arrayOf(".Data"), relativePaths: Boolean = false) {
    fun String.toFile(): File = if (relativePaths) File(getTestDataPath(), this) else File(this)

    val noExtensionPath = FileUtil.getNameWithoutExtension(path)
    val extensions = arrayOf("kt", "java")
    val extraPaths: List<String> = extraNameParts
            .flatMap { extensions.map { ext -> "$noExtensionPath$it.$ext" } }
            .filter { it.toFile().exists() }

    configureByFiles(*(listOf(path) + extraPaths).toTypedArray())
}

public fun String.trimIndent(): String {
    val lines = split('\n')

    val firstNonEmpty = lines.firstOrNull { !it.trim().isEmpty() }
    if (firstNonEmpty == null) {
        return this
    }

    val trimmedPrefix = firstNonEmpty.takeWhile { ch -> ch.isWhitespace() }
    if (trimmedPrefix.isEmpty()) {
        return this
    }

    return lines.map { line ->
        if (line.trim().isEmpty()) {
            ""
        }
        else {
            if (!line.startsWith(trimmedPrefix)) {
                throw IllegalArgumentException(
                        """Invalid line "$line", ${trimmedPrefix.length()} whitespace character are expected""")
            }

            line.substring(trimmedPrefix.length())
        }
    }.joinToString(separator = "\n")
}

public fun PsiFile.findElementByCommentPrefix(commentText: String): PsiElement? =
        findElementsByCommentPrefix(commentText).keySet().singleOrNull()

public fun PsiFile.findElementsByCommentPrefix(prefix: String): Map<PsiElement, String> {
    var result = SmartFMap.emptyMap<PsiElement, String>()
    accept(
            object : JetTreeVisitorVoid() {
                override fun visitComment(comment: PsiComment) {
                    val commentText = comment.getText()
                    if (commentText.startsWith(prefix)) {
                        val parent = comment.getParent()
                        val elementToAdd = when (parent) {
                            is JetDeclaration -> parent
                            is PsiMember -> parent
                            else -> PsiTreeUtil.skipSiblingsForward(
                                    comment,
                                    javaClass<PsiWhiteSpace>(), javaClass<PsiComment>(), javaClass<JetPackageDirective>()
                            )
                        } as? PsiElement ?: return

                        result = result.plus(elementToAdd, commentText.substring(prefix.length()).trim())
                    }
                }
            }
    )
    return result
}