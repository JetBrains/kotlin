/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.context.ContextUpdater
import org.jetbrains.kotlin.backend.common.checkers.context.ValueScopeUpdater
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression

object IrValueAccessScopeChecker : IrElementChecker<IrValueAccessExpression>(IrValueAccessExpression::class) {
    override val requiredContextUpdaters: Set<ContextUpdater>
        get() = setOf(ValueScopeUpdater)

    override fun check(element: IrValueAccessExpression, context: CheckerContext) {
        if (!context.valueSymbolScopeStack.isVisibleInCurrentScope(element.symbol)) {
            context.error(element, "The following expression references a value that is not available in the current scope.")
        }
    }
}