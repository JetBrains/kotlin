/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.context

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.utils.addIfNotNull

object ValueScopeUpdater : ContextUpdater {
    override fun runInNewContext(
        context: CheckerContext,
        element: IrElement,
        block: () -> Unit,
    ) {
        when (element) {
            is IrValueDeclaration -> {
                context.valueSymbolScopeStack.addToCurrentScope(element.symbol)
                block()
            }
            is IrClass -> {
                context.withScopeOwner(element, block) {
                    // By default, `thisReceiver` is always visited _after_ the child declarations (where it may be referenced),
                    // so we add it manually before.
                    addIfNotNull(element.thisReceiver?.symbol)
                }
            }
            is IrScript -> {
                context.withScopeOwner(element, block) {
                    // By default, `thisReceiver` is always visited _after_ the script statements (where it may be referenced),
                    // so we add it manually before.
                    addIfNotNull(element.thisReceiver?.symbol)
                    addAll(element.implicitReceiversParameters.map { it.symbol })
                    addAll(element.explicitCallParameters.map { it.symbol })
                }
            }
            is IrReplSnippet -> {
                context.withScopeOwner(element, block) {
                    element.variablesFromOtherSnippets.mapTo(this, IrVariable::symbol)
                }
            }
            is IrFunction -> {
                context.withScopeOwner(element, block) {
                    // A function parameter's default value may reference the parameters that come after it,
                    // so we add all the parameters to the scope manually before validating any of them
                    element.parameters.mapTo(this, IrValueParameter::symbol)
                }
            }
            is IrAnonymousInitializer -> {
                context.withScopeOwner(element, block) {
                    addValueParametersOfPrimaryConstructor(element)
                }
            }
            is IrField -> {
                context.withScopeOwner(element, block) {
                    addValueParametersOfPrimaryConstructor(element)
                }
            }
            is IrCatch -> {
                // catchParameter only has scope over result expression, so create a new scope
                context.withScopeOwner(element, block)
            }
            is IrBlock -> {
                // Entering a new scope
                context.withScopeOwner(element, block)
            }
            is IrElement -> {
                block()
            }
        }
    }

    private fun MutableSet<IrValueSymbol>.addValueParametersOfPrimaryConstructor(declaration: IrDeclaration) {
        (declaration.parent as? IrClass)?.primaryConstructor?.parameters?.mapTo(this, IrValueParameter::symbol)
    }
}
