/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.resolution.KtResolvable

/**
 * Represents a reference to a label in a labeled expression, such as `@main` in `return@main`.
 *
 * ### Example:
 *
 * ```kotlin
 * fun main() {
 *     return@main
 * //        ^___^
 * }
 * ```
 */
@OptIn(KtExperimentalApi::class)
class KtLabelReferenceExpression(node: ASTNode) : KtSimpleNameExpressionImpl(node), KtResolvable {
    override fun getReferencedNameElement() = getIdentifier() ?: this
}
