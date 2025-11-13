/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.source

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentLhsIfUnwrappable

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