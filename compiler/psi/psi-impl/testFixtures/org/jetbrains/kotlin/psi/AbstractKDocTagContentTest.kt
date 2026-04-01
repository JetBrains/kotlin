/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractKDocTagContentTest : AbstractPsiBasedTest() {
    override fun doTest(file: KtFile, testServices: TestServices) {
        val actual = buildString {
            file.acceptChildren(
                object : KtTreeVisitorVoid() {
                    override fun visitElement(element: PsiElement) {
                        element.acceptChildren(this)

                        if (element !is KDocTag) {
                            return
                        }
                        appendLine("ORIGINAL KDOC:")
                        appendLine(element.text)
                        appendLine("CONTENT:")
                        appendLine(element.getContent())
                        appendLine()
                    }
                }
            )
        }

        assertEqualsToTestOutputFile(actual, ".tagContent.txt")
    }
}
