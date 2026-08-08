/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.analysis.api.impl.base.util.requireIsInstance
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.internal.utils.IndentedTextBuilder
import org.jetbrains.kotlin.analysis.internal.utils.buildIndentedText
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Dumps the decompiled text of [files] and validates that it has no syntax errors.
 *
 * [files] have to be [decompiled files][KtDecompiledFile], so the check is only applicable to binary test modules.
 */
context(testContext: AbstractAnalysisApiBasedTest)
internal fun checkDecompiledText(files: List<KtFile>, testServices: TestServices) {
    val sortedFiles = files.sortedBy(KtFile::getName)

    val actual = buildIndentedText(indentation = IndentedTextBuilder.TWO_SPACES) {
        if (sortedFiles.isEmpty()) {
            appendLine("NO FILES")
            return@buildIndentedText
        }

        val singleFile = sortedFiles.singleOrNull()
        if (singleFile != null) {
            append(singleFile.text)
        } else {
            appendCollection(sortedFiles, separator = "\n") { file ->
                appendLine("${file.name}:")
                withIndent {
                    append(file.text)
                }
            }
        }
    }

    testContext.assertEqualsToTestOutputFile(actual, extension = ".decompiledText.txt")

    for (file in sortedFiles) {
        requireIsInstance<KtDecompiledFile>(file)
        file.validateTree(testServices.assertions)
    }
}

/**
 * Checks that the decompiled text of the file is parsed without syntax errors.
 */
fun KtDecompiledFile.validateTree(assertions: Assertions) {
    val visitor = object : KtTreeVisitorVoid() {
        override fun visitErrorElement(element: PsiErrorElement) {
            assertions.fail {
                val parent = element.parent
                """
                    Decompiled file should not contain syntax errors!
                    Parent class: ${parent::class.simpleName}
                    Parent text: ${parent.text}
                """.trimIndent()
            }
        }
    }

    accept(visitor)
}
