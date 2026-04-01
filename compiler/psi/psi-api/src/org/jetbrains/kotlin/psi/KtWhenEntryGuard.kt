/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode

/**
 * Represents a guard condition in a `when` entry using the `if` keyword.
 *
 * ### Example:
 *
 * ```kotlin
 * when (x) {
 *     is String if x.isNotEmpty() -> println(x)
 * //            ^_______________^
 * }
 * ```
 */
class KtWhenEntryGuard(node: ASTNode) : KtElementImpl(node) {
    fun getExpression(): KtExpression? = findChildByClass(KtExpression::class.java)
}
