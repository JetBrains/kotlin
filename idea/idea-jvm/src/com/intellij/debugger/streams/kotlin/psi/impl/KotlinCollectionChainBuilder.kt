// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.impl

import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionChainBuilder : KotlinChainBuilderBase(TODO("transmit here a correct transformer")) {
  override val existenceChecker: ExistenceChecker = object : ExistenceChecker() {
    override fun visitCallExpression(expression: KtCallExpression) {
      if (isFound()) return
      if (isCollectionTransformationCall(expression)) {
        fireElementFound()
      } else {
        super.visitCallExpression(expression)
      }
    }

    private fun isCollectionTransformationCall(expression: KtCallExpression): Boolean {
      TODO("determine that expression is a collection transformation")
    }
  }

  override fun createChainsBuilder(): ChainBuilder = object : ChainBuilder() {
    private val previousCalls: MutableMap<KtCallExpression, KtCallExpression> = mutableMapOf()

    override fun visitCallExpression(expression: KtCallExpression) {
      super.visitCallExpression(expression)
    }

    override fun chains(): List<List<KtCallExpression>> {
      return emptyList()
    }
  }
}