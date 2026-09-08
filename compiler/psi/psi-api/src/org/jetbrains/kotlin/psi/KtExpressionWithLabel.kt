/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.Name

/**
 * Represents an expression that may contain a label qualifier. A [KtLabeledExpression] declares a label with `label@`, while labeled
 * `break`/`continue`, qualified `return`, and qualified `this`/`super` expressions reference one with `@label`.
 *
 * ### Example:
 *
 * ```kotlin
 * loop@ for (i in 1..10) {
 *     if (i > 5) break@loop
 * //             ^________^
 * // A labeled 'break' expression; '@loop' is the label reference
 * }
 * ```
 */
@SubclassOptInRequired(KtImplementationDetail::class)
open class KtExpressionWithLabel : KtExpressionImpl {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    /**
     * The label's simple-name expression, or `null` if this expression has no label.
     */
    fun getTargetLabel(): KtSimpleNameExpression? {
        return labelQualifier?.findChildByType(KtNodeTypes.LABEL)
    }

    /**
     * The container node that wraps the label qualifier (`label@` or `@label`), or `null` if this expression has no label.
     */
    val labelQualifier: KtContainerNode?
        get() = findChildByType(KtNodeTypes.LABEL_QUALIFIER)

    /**
     * Returns the label name without the `@` sign, or `null` if this expression has no label.
     */
    fun getLabelName(): String? = getTargetLabel()?.getReferencedName()

    /**
     * Returns the label name as a [Name], or `null` if this expression has no label.
     */
    fun getLabelNameAsName(): Name? = getTargetLabel()?.getReferencedNameAsName()

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitExpressionWithLabel(this, data)
}
