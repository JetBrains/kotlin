/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes

open class LightTreePositioningStrategy {
    open fun markDiagnostic(diagnostic: FirDiagnostic<*>): List<TextRange> {
        val element = diagnostic.element
        return mark(element.lighterASTNode, element.treeStructure)
    }

    open fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
        return markElement(node, tree)
    }

    companion object {
        val DEFAULT = object : LightTreePositioningStrategy() {
            override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
                when (node.tokenType) {
                    KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL -> {
                        return LightTreePositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL.mark(node, tree)
                    }
                }
                return super.mark(node, tree)
            }
        }
    }
}

fun markElement(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
    return listOf(TextRange(tree.getStartOffset(node), tree.getEndOffset(node)))
}

fun markRange(from: LighterASTNode, to: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
    return listOf(TextRange(tree.getStartOffset(from), tree.getEndOffset(to)))
}
