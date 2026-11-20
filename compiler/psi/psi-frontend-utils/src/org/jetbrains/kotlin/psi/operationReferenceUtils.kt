/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.psi.utils.OperatorTokens

/**
 * Returns `true` if this operation sign corresponds to a convention operator that maps to a named operator function (for example, `+`
 * maps to `plus`). Returns `false` for non-convention signs such as `&&`.
 */
fun KtOperationReferenceExpression.isConventionOperator(): Boolean {
    val tokenType = operationSignTokenType ?: return false
    return OperatorTokens.operationName(tokenType) != null
}
