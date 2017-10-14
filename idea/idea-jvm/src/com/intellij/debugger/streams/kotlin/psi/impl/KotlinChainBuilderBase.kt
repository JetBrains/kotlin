/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.psi.ChainTransformer
import com.intellij.debugger.streams.psi.PsiUtil
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

/**
 * @author Vitaliy.Bibaev
 */
abstract class KotlinChainBuilderBase(private val transformer: ChainTransformer<KtCallExpression>) : StreamChainBuilder {
  protected abstract val existenceChecker: ExistenceChecker

  override fun isChainExists(startElement: PsiElement): Boolean {
    var element: PsiElement? = getLatestElementInScope(PsiUtil.ignoreWhiteSpaces(startElement))
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
    var element = getLatestElementInScope(PsiUtil.ignoreWhiteSpaces(startElement))
    while (element != null) {
      element.accept(visitor)
      element = getLatestElementInScope(toUpperLevel(element))
    }

    return visitor.chains().map { transformer.transform(it, startElement) }
  }

  private fun toUpperLevel(element: PsiElement): PsiElement? {
    var current = element.parent

    while (current != null && !(current is KtLambdaExpression || current is KtAnonymousInitializer)) {
      current = current.parent
    }

    return current
  }

  protected abstract fun createChainsBuilder(): ChainBuilder

  private fun getLatestElementInScope(element: PsiElement?): PsiElement? {
    var current = element
    while (current != null) {
      val parent = current.parent
      if (parent is KtBlockExpression || parent is KtLambdaExpression || parent is KtStatementExpression) {
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