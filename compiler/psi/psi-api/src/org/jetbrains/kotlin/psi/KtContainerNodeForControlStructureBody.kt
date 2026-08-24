/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode

/**
 * A wrapper node that holds the body of a control structure, such as an `if`/`else` branch or a loop body.
 *
 * The wrapper lets a branch or loop body be either a block or a single statement while keeping a uniform tree shape, so consumers can
 * always reach the body through this node.
 *
 * ### Example:
 *
 * ```kotlin
 * if (condition) doSomething() else doSomethingElse()
 * //             ^___________^      ^_______________^
 * // Each branch body is wrapped in a KtContainerNodeForControlStructureBody
 * ```
 */
@OptIn(KtImplementationDetail::class)
class KtContainerNodeForControlStructureBody : KtContainerNode {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    /**
     * The body expression held by this container, or `null` if it is absent in incomplete code.
     */
    val expression: KtExpression?
        get() = findChildByClass<KtExpression>(KtExpression::class.java)
}
