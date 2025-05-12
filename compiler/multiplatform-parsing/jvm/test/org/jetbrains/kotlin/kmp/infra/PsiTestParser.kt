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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class PsiTestParser(parseMode: ParseMode) : AbstractTestParser<PsiElement>(parseMode) {
    companion object {
        private val disposable = Disposer.newDisposable("Disposable for the ${PsiTestParser::class.simpleName}")
        private val environment: KotlinCoreEnvironment =
            KotlinCoreEnvironment.createForTests(disposable, CompilerConfiguration.EMPTY, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        private val ktPsiFactory = KtPsiFactory(environment.project)
    }

    init {
        require(parseMode == ParseMode.KDocOnly || parseMode == ParseMode.Full) {
            "${PsiTestParser::class.simpleName} currently supports only ${ParseMode.KDocOnly::class.simpleName} and ${ParseMode.Full::class.simpleName} modes"
        }
    }

    override fun parse(fileName: String, text: String): TestParseNode<out PsiElement> {
        return ktPsiFactory.createFile(fileName, text).toTestParseTree(kDocOnly = parseMode == ParseMode.KDocOnly)
            .wrapRootsIfNeeded(text.length)
    }

    private fun PsiElement.toTestParseTree(kDocOnly: Boolean, insideKDoc: Boolean = false): List<TestParseNode<PsiElement>> {
        val kDocStartOrInside = elementType == KtTokens.DOC_COMMENT || insideKDoc
        return when {
            !kDocOnly || kDocStartOrInside -> {
                listOf(
                    TestParseNode(
                        elementType.toString(),
                        startOffset,
                        startOffset + textLength,
                        this,
                        convertChildren(kDocOnly = kDocOnly, insideKDoc = kDocStartOrInside)
                    )
                )
            }
            else -> {
                // Flat map children for ignored elements
                convertChildren(kDocOnly = true, insideKDoc = false)
            }
        }
    }

    fun PsiElement.convertChildren(kDocOnly: Boolean, insideKDoc: Boolean = false): List<TestParseNode<PsiElement>> {
        return buildList {
            // Note: use traverse with `nextSibling`
            // because in some implementations `children` are only composite elements, i.e., not leaf elements (see docs)
            // The main purpose is to extract a full-fidelity tree for fully-fledged comparison.
            var currentChild = firstChild
            while (currentChild != null) {
                addAll(currentChild.toTestParseTree(kDocOnly, insideKDoc))
                currentChild = currentChild.nextSibling
            }
        }
    }
}