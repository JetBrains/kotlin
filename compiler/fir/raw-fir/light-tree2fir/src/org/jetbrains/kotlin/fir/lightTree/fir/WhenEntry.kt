/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.buildBalancedOrExpressionTree
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildErrorExpression
import org.jetbrains.kotlin.toKtLightSourceElement

class WhenEntry<Node : Any>(
    val conditions: List<FirExpression>,
    val guard: FirExpression?,
    val firBlock: FirBlock,
    val node: Node,
    val isElse: Boolean = false,
    val shouldBindSubject: Boolean = false,
    val sourceElement: KtSourceElement? = null,
) {
    fun toFirWhenCondition(): FirExpression {
        require(conditions.isNotEmpty())
        return buildBalancedOrExpressionTree(conditions)
    }

    fun toFirWhenConditionWithoutSubject(): FirExpression {
        return when (conditions.size) {
            0 -> buildErrorExpression(sourceElement, ConeSyntaxDiagnostic("No expression in condition with expression"))
            else -> buildBalancedOrExpressionTree(conditions)
        }
    }
}
