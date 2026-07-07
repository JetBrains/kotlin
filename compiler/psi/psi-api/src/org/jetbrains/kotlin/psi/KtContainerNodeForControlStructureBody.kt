/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode

/**
 * A wrapper node that holds the body of a control structure, such as an `if`/`else` branch or a loop body.
 *
 * The wrapper lets a branch or loop body be either a block or a single statement while keeping a uniform tree shape,
 * so consumers can always reach the body through this node.
 *
 * ### Example:
 *
 * ```kotlin
 * if (condition) doSomething() else doSomethingElse()
 * //             ^___________^      ^_______________^
 * // Each branch body is wrapped in a KtContainerNodeForControlStructureBody
 * ```
 */
class KtContainerNodeForControlStructureBody(node: ASTNode) : KtContainerNode(node) {
    /**
     * The body expression held by this container, or `null` if it is absent in incomplete code.
     */
    val expression: KtExpression?
        get() = findChildByClass<KtExpression>(KtExpression::class.java)
}
