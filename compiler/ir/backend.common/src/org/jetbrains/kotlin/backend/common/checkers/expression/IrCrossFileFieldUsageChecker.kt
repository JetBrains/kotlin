/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.util.fileOrNull

/**
 * Makes sure that [IrField]s are not accessed outside their containing files.
 */
internal object IrCrossFileFieldUsageChecker : IrFieldAccessChecker {
    override fun check(
        expression: IrFieldAccessExpression,
        context: CheckerContext,
    ) {
        val field = expression.symbol.owner
        if (field.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return
        val containingFile = field.fileOrNull ?: return

        if (containingFile != context.file) {
            context.error(
                expression,
                "Access to a field declared in another file: ${containingFile.path}",
            )
        }
    }
}