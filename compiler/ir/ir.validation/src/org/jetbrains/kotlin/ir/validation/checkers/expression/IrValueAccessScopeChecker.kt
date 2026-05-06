/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.SCRIPT_K2_ORIGIN
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
            if (declaration is IrValueParameter) {
                if (declaration.origin == IrDeclarationOrigin.INSTANCE_RECEIVER &&
                    declaration.parents.any { (it as? IrClass)?.origin == IrDeclarationOrigin.SCRIPT_CLASS }
                ) {
                    // Invalid references to a different script instance are instead reported in the scripting plugin,
                    // with a more accurate error message.
                    return
                }
                if (declaration.origin == IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER &&
                    (declaration.parent as? IrScript)?.origin == SCRIPT_K2_ORIGIN
                ) {
                    // May happen with scripts with custom configuration
                    // Implicit receiver appears only at stage of the script running
                    return
                }
            }
            if (declaration is IrVariable
                && declaration.origin == IrDeclarationOrigin.SCRIPT_CALL_PARAMETER
                && (declaration.parent as? IrScript)?.origin == SCRIPT_K2_ORIGIN
            ) {
                // May happen with scripts with custom configuration
                // Parameter appears only at stage of the script running
                return
            }

            context.error(element, "The following expression references a value that is not available in the current scope.")
        }
    }
}
