// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.kotlin.psi.StreamCallChecker
import com.intellij.debugger.streams.kotlin.psi.java.JavaStreamChainTypeExtractor
import com.intellij.debugger.streams.kotlin.psi.previousCall
import org.jetbrains.kotlin.psi.KtCallExpression
import java.util.*

/**
 * @author Vitaliy.Bibaev
 */
class KotlinJavaStreamChainBuilder(private val callChecker: StreamCallChecker)
  : KotlinChainBuilderBase(KotlinChainTransformerImpl(JavaStreamChainTypeExtractor())) {
  override val existenceChecker: ExistenceChecker = MyExistenceChecker()

  override fun createChainsBuilder(): ChainBuilder = MyBuilderVisitor()

  private inner class MyExistenceChecker : ExistenceChecker() {
    override fun visitCallExpression(expression: KtCallExpression) {
      if (callChecker.isTerminationCall(expression)) {
        fireElementFound()
      } else {
        super.visitCallExpression(expression)
      }
    }
  }

  private inner class MyBuilderVisitor : ChainBuilder() {
    private val myTerminationCalls = mutableSetOf<KtCallExpression>()
    private val myPreviousCalls = mutableMapOf<KtCallExpression, KtCallExpression>()

    override fun visitCallExpression(expression: KtCallExpression) {
      super.visitCallExpression(expression)
      if (!myPreviousCalls.containsKey(expression) && callChecker.isStreamCall(expression)) {
        updateCallTree(expression)
      }
    }

    private fun updateCallTree(expression: KtCallExpression) {
      if (callChecker.isTerminationCall(expression)) {
        myTerminationCalls.add(expression)
      }

      val parentCall = expression.previousCall()
      if (parentCall is KtCallExpression && callChecker.isStreamCall(parentCall)) {
        myPreviousCalls.put(expression, parentCall)
        updateCallTree(parentCall)
      }
    }

    override fun chains(): List<List<KtCallExpression>> {
      val chains = ArrayList<List<KtCallExpression>>()
      for (terminationCall in myTerminationCalls) {
        val chain = ArrayList<KtCallExpression>()
        var current: KtCallExpression? = terminationCall
        while (current != null) {
          if (!callChecker.isStreamCall(current)) {
            break
          }
          chain.add(current)
          current = myPreviousCalls[current]
        }

        Collections.reverse(chain)
        chains.add(chain)
      }

      return chains
    }
  }
}
