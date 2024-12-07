/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.psi.*

fun KtElement?.getCalleeExpressionIfAny(): KtExpression? =
    when (val element = if (this is KtExpression) KtPsiUtil.deparenthesize(this) else this) {
        is KtSimpleNameExpression -> element
        is KtCallElement -> element.calleeExpression
        is KtQualifiedExpression -> element.selectorExpression.getCalleeExpressionIfAny()
        is KtOperationExpression -> element.operationReference
        else -> null
    }
