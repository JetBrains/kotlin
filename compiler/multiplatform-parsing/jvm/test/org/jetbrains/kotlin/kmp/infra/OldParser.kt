/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class OldParser : AbstractParser<PsiElement>() {
    companion object {
        private val disposable = Disposer.newDisposable("Disposable for the ${OldParser::class.simpleName}")
        private val environment: KotlinCoreEnvironment =
            KotlinCoreEnvironment.createForTests(disposable, CompilerConfiguration.EMPTY, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        private val ktPsiFactory = KtPsiFactory(environment.project)
    }

    override fun parseKDocOnlyNodes(fileName: String, text: String): List<TestParseNode<PsiElement>> {
        return ktPsiFactory.createFile(fileName, text).toParseTree(kDocOnly = true)
    }

    override fun parse(fileName: String, text: String): TestParseNode<PsiElement> {
        return ktPsiFactory.createFile(fileName, text).toParseTree(kDocOnly = false).single()
    }

    private fun PsiElement.toParseTree(kDocOnly: Boolean, insideKDoc: Boolean = false): List<TestParseNode<PsiElement>> {
        if (!kDocOnly) {
            return listOf(
                TestParseNode(
                    elementType.toString(),
                    startOffset,
                    startOffset + textLength,
                    this,
                    collectChildren(kDocOnly = false)
                )
            )
        }

        // Ignore `MARKDOWN_LINK` for now because another sub-parser handles them
        val kDocStop = elementType == KDocTokens.MARKDOWN_LINK
        return when {
            elementType == KtTokens.DOC_COMMENT || insideKDoc || kDocStop -> {
                listOf(
                    TestParseNode(
                        elementType.toString(),
                        startOffset,
                        startOffset + textLength,
                        this,
                        collectChildren(kDocOnly = true, insideKDoc = !kDocStop)
                    )
                )
            }
            else -> {
                // Flat map children for ignored elements
                collectChildren(kDocOnly = true, insideKDoc = false)
            }
        }
    }

    fun PsiElement.collectChildren(kDocOnly: Boolean, insideKDoc: Boolean = false): List<TestParseNode<PsiElement>> {
        return buildList {
            // Note: use traverse with `nextSibling`
            // because in some implementations `children` are only composite elements, i.e., not leaf elements (see docs)
            // The main purpose is to extract a full-fidelity tree for fully-fledged comparison.
            var currentChild = firstChild
            while (currentChild != null) {
                addAll(currentChild.toParseTree(kDocOnly, insideKDoc))
                currentChild = currentChild.nextSibling
            }
        }
    }
}