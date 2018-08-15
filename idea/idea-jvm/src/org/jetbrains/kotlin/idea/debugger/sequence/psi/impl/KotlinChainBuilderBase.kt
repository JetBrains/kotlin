// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.impl

import com.intellij.debugger.streams.psi.ChainTransformer
import com.intellij.debugger.streams.psi.PsiUtil
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.*

abstract class KotlinChainBuilderBase(private val transformer: ChainTransformer<KtCallExpression>) : StreamChainBuilder {
    protected abstract val existenceChecker: ExistenceChecker

    override fun isChainExists(startElement: PsiElement): Boolean {
        val start = if (startElement is PsiWhiteSpace) PsiUtil.ignoreWhiteSpaces(startElement) else startElement
        var element = getLatestElementInScope(start)
        existenceChecker.reset()
        while (element != null && !existenceChecker.isFound()) {
            existenceChecker.reset()
            element.accept(existenceChecker)
            element = toUpperLevel(element)
        }

        return existenceChecker.isFound()
    }

    override fun build(startElement: PsiElement): List<StreamChain> {
        val visitor = createChainsBuilder()
        val start = if (startElement is PsiWhiteSpace) PsiUtil.ignoreWhiteSpaces(startElement) else startElement
        var element = getLatestElementInScope(start)
        while (element != null) {
            element.accept(visitor)
            element = toUpperLevel(element)
        }

        return visitor.chains().map { transformer.transform(it, startElement) }
    }

    private fun toUpperLevel(element: PsiElement): PsiElement? {
        var current = element.parent

        while (current != null && !(current is KtLambdaExpression || current is KtAnonymousInitializer || current is KtObjectDeclaration)) {
            current = current.parent
        }

        return getLatestElementInScope(current)
    }

    protected abstract fun createChainsBuilder(): ChainBuilder

    private fun getLatestElementInScope(element: PsiElement?): PsiElement? {
        var current = element
        while (current != null) {
            if (current is KtNamedFunction && current.hasInitializer()) {
                break
            }

            val parent = current.parent
            if (parent is KtBlockExpression || parent is KtLambdaExpression) {
                break
            }


            current = parent
        }

        return current
    }

    protected abstract class ExistenceChecker : MyTreeVisitor() {
        private var myIsFound: Boolean = false
        fun isFound(): Boolean = myIsFound
        fun reset() = setFound(false)
        protected fun fireElementFound() = setFound(true)

        private fun setFound(value: Boolean) {
            myIsFound = value
        }
    }

    protected abstract class ChainBuilder : MyTreeVisitor() {
        abstract fun chains(): List<List<KtCallExpression>>
    }

    protected abstract class MyTreeVisitor : KtTreeVisitorVoid() {
        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {}
        override fun visitBlockExpression(expression: KtBlockExpression) {}
    }
}