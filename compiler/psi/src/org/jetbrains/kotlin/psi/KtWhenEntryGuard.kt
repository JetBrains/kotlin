/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode

class KtWhenEntryGuard(node: ASTNode) : KtElementImpl(node) {
    fun getExpression(): KtExpression? = findChildByClass(KtExpression::class.java)
}
