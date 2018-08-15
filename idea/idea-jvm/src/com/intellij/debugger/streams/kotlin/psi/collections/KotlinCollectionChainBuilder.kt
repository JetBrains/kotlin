// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.collections

import com.intellij.debugger.streams.kotlin.psi.KotlinPsiUtil
import com.intellij.debugger.streams.kotlin.psi.impl.KotlinChainBuilderBase
import com.intellij.debugger.streams.kotlin.psi.impl.KotlinChainTransformerImpl
import com.intellij.debugger.streams.kotlin.psi.previousCall
import com.intellij.debugger.streams.kotlin.psi.receiverType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionChainBuilder
  : KotlinChainBuilderBase(KotlinChainTransformerImpl(KotlinCollectionsTypeExtractor())) {
  private companion object {
    // TODO: Avoid enumeration of all available types
    val SUPPORTED_RECEIVERS = setOf("kotlin.collections.Iterable", "kotlin.CharSequence", "kotlin.Array",
        "kotlin.BooleanArray", "kotlin.ByteArray", "kotlin.ShortArray", "kotlin.CharArray", "kotlin.IntArray",
        "kotlin.LongArray", "kotlin.DoubleArray", "kotlin.FloatArray")
  }

  private fun isCollectionTransformationCall(expression: KtCallExpression): Boolean {
    val receiverType = expression.receiverType() ?: return false
    if (isTypeSuitable(receiverType)) return true
    return receiverType.supertypes().any { isTypeSuitable(it) }
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

  private fun isTypeSuitable(type: KotlinType): Boolean =
      SUPPORTED_RECEIVERS.contains(KotlinPsiUtil.getTypeWithoutTypeParameters(type))
}