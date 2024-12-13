/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.ensureBound
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.types.IrDynamicType

internal object IrDeclarationReferenceBoundChecker : IrDeclarationReferenceChecker {
    override fun check(
        expression: IrDeclarationReference,
        context: CheckerContext,
    ) {
        // TODO: Fix unbound dynamic filed declarations
        if (expression is IrFieldAccessExpression) {
            val receiverType = expression.receiver?.type
            if (receiverType is IrDynamicType)
                return
        }

        expression.symbol.ensureBound(expression, context)
    }
}