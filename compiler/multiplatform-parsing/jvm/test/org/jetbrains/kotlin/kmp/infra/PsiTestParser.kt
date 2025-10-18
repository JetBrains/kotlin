/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class PsiTestParser : AbstractTestParser<PsiElement>(ParseMode.Full), Disposable {
    private val disposable = Disposer.newDisposable("Disposable for the ${PsiTestParser::class.simpleName}")

    @OptIn(K1Deprecation::class)
    private val environment: KotlinCoreEnvironment =
        KotlinCoreEnvironment.createForTests(disposable, CompilerConfiguration.EMPTY, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    private val ktPsiFactory = KtPsiFactory(environment.project)

    override fun parse(fileName: String, text: String): TestParseNode<out PsiElement> {
        return ktPsiFactory.createFile(fileName, text).toTestParseTree().wrapRootsIfNeeded(text.length)
    }

    private fun PsiElement.toTestParseTree(): List<TestParseNode<PsiElement>> {
        val children = buildList {
            // Note: use traverse with `nextSibling`
            // because in some implementations `children` are only composite elements, i.e., not leaf elements (see docs)
            // The main purpose is to extract a full-fidelity tree for fully-fledged comparison.
            var currentChild = this@toTestParseTree.firstChild
            while (currentChild != null) {
                addAll(currentChild.toTestParseTree())
                currentChild = currentChild.nextSibling
            }
        }

        return listOf(
            TestParseNode(
                elementType.toString(),
                startOffset,
                startOffset + textLength,
                this,
                children
            )
        )
    }

    override fun dispose() {
        ApplicationManager.getApplication().invokeLater {
            Disposer.dispose(disposable)
        }
    }
}
