/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.ensureBound
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference

internal object IrLocalDelegatedPropertyReferenceBoundChecker : IrLocalDelegatedPropertyReferenceChecker {
    override fun check(
        expression: IrLocalDelegatedPropertyReference,
        context: CheckerContext,
    ) {
        expression.delegate.ensureBound(expression, context)
        expression.getter.ensureBound(expression, context)
        expression.setter?.ensureBound(expression, context)
    }
}