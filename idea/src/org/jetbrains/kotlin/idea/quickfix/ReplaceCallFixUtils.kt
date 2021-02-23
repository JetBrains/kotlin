/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

fun KtExpression.shouldHaveNotNullType(): Boolean {
    val type = when (val parent = parent) {
        is KtBinaryExpression -> parent.left?.let { it.getType(it.analyze()) }
        is KtProperty -> parent.typeReference?.let { it.analyze()[BindingContext.TYPE, it] }
        else -> null
    } ?: return false
    return !type.isMarkedNullable
}
