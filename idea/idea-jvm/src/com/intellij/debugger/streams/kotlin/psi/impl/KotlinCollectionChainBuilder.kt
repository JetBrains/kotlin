// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.kotlin.psi.previousCall
import com.intellij.debugger.streams.kotlin.psi.receiverType
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionChainBuilder : KotlinChainBuilderBase(KotlinChainTransformerImpl()) {
  private companion object {
    // TODO: Avoid enumeration of all available types
    val SUPPORTED_RECEIVERS = setOf("kotlin.collections.List", "kotlin.collections.Set")
  }

  private fun isCollectionTransformationCall(expression: KtCallExpression): Boolean {
    val receiverType = expression.receiverType() ?: return false
    val receiverTypeName = receiverType.getJetTypeFqName(false)
    return SUPPORTED_RECEIVERS.contains(receiverTypeName)
  }

  override val existenceChecker: ExistenceChecker = object : ExistenceChecker() {
    override fun visitCallExpression(expression: KtCallExpression) {
      if (isFound()) return
      if (isCollectionTransformationCall(expression)) {
        fireElementFound()
      } else {
        super.visitCallExpression(expression)
      }
    }


  }

  override fun createChainsBuilder(): ChainBuilder = object : ChainBuilder() {
    private val previousCalls: MutableMap<KtCallExpression, KtCallExpression> = mutableMapOf()
    private val visitedCalls: MutableSet<KtCallExpression> = mutableSetOf()

    override fun visitCallExpression(expression: KtCallExpression) {
      super.visitCallExpression(expression)
      if (isCollectionTransformationCall(expression)) {
        visitedCalls.add(expression)
        val previous = expression.previousCall()
        if (previous != null && isCollectionTransformationCall(previous)) {
          previousCalls[expression] = previous
        }
      }
    }

    override fun chains(): List<List<KtCallExpression>> {
      val notLastCalls: Set<KtCallExpression> = previousCalls.values.toSet()
      return visitedCalls.filter { it !in notLastCalls }.map { buildPsiChain(it) }
    }

    private fun buildPsiChain(expression: KtCallExpression): List<KtCallExpression> {
      val result = mutableListOf<KtCallExpression>()
      var current: KtCallExpression? = expression
      while (current != null) {
        result.add(current)
        current = previousCalls[current]
      }

      result.reverse()

      return result
    }
  }
}