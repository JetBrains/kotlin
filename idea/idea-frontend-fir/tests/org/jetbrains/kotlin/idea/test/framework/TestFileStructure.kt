/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test.framework

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

class TestFileStructure(
    val filePath: Path,
    val caretPosition: Int?,
    val directives: TestFileDirectives,
    val mainFile: TestFile.KtTestFile,
    val otherFiles: List<TestFile>,
) {
    val mainKtFile: KtFile
        get() = mainFile.psiFile

    val allFiles: List<TestFile> = listOf(mainFile) + otherFiles
}

data class TestStructureExpectedDataBlock(val name: String, val values: List<String>)

class TestFileDirectives(
    private val directives: Map<String, Any>
) {
    fun <VALUE : Any, DIRECTIVE : TestFileDirective<VALUE>> getDirectiveValueIfPresent(directive: DIRECTIVE): VALUE? {
        val value = directives[directive.name] ?: return null
        @Suppress("UNCHECKED_CAST")
        return value as VALUE
    }
}

abstract class TestFileDirective<VALUE : Any> {
    abstract val name: String
    abstract fun parse(value: String): VALUE?
}

sealed class TestFile {
    abstract val psiFile: PsiFile

    data class KtTestFile(override val psiFile: KtFile) : TestFile()
    data class JavaTestFile(override val psiFile: PsiJavaFile) : TestFile()

    companion object {
        fun createByPsiFile(psiFile: PsiFile) = when (psiFile) {
            is KtFile -> KtTestFile(psiFile)
            is PsiJavaFile -> JavaTestFile(psiFile)
            else -> error("Unknown file type ${psiFile::class}")
        }
    }
}

