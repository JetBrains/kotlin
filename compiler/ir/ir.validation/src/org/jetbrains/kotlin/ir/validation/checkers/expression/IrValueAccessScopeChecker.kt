/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.context.ContextUpdater
import org.jetbrains.kotlin.ir.validation.checkers.context.ValueScopeUpdater

object IrValueAccessScopeChecker : IrElementChecker<IrValueAccessExpression>(IrValueAccessExpression::class) {
    override val requiredContextUpdaters: Set<ContextUpdater>
        get() = setOf(ValueScopeUpdater)

    override fun check(element: IrValueAccessExpression, context: CheckerContext) {
        if (!context.valueSymbolScopeStack.isVisibleInCurrentScope(element.symbol)) {
            val declaration = element.symbol.owner
            if (declaration is IrValueParameter &&
                declaration.origin == IrDeclarationOrigin.INSTANCE_RECEIVER &&
                declaration.parents.any { (it as? IrClass)?.origin == IrDeclarationOrigin.SCRIPT_CLASS }
            ) {
                // Invalid references to a different script instance are instead reported in the scripting plugin,
                // with a more accurate error message.
                return
            }

            context.error(element, "The following expression references a value that is not available in the current scope.")
        }
    }
}