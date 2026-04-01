/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.source

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.PREFIX_EXPRESSION
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentLhsIfUnwrappable
import org.jetbrains.kotlin.util.getChildren

/**
 * This function should only be called for a source element corresponding to
 * an assignment/assignment operator call/increment or a decrement operator.
 */
fun KtSourceElement?.hasUnwrappableAsAssignmentLhs(): Boolean {
    if (this == null) {
        return false
    }

    val node = psi?.getAssignmentLhsIfUnwrappable()
        ?: lighterASTNode.getAssignmentLhsIfUnwrappable(treeStructure)

    return node != null
}

/**
 * This function should only be called for a source element corresponding to
 * an assignment/assignment operator call/increment or a decrement operator.
 */
fun LighterASTNode.getAssignmentLhsIfUnwrappable(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? =
    when {
        // In `++(x)` the LHS source `(x)` is the last child
        tokenType == PREFIX_EXPRESSION -> getChildren(tree).lastOrNull()
        // In `(x)++` or `(x) = ...` the LHS source is the first child
        else -> getChildren(tree).firstOrNull()
    }.takeIf {
        it?.tokenType in UNWRAPPABLE_TOKEN_TYPES
    }