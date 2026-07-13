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
    override fun enterContext(context: CheckerContext, element: IrElement) {
        when (element) {
            is IrValueDeclaration -> {
                context.valueSymbolScopeStack.addToCurrentScope(element.symbol)
            }
            is IrClass -> {
                context.enterNewScopeForOwner(element) {
                    // By default, `thisReceiver` is always visited _after_ the child declarations (where it may be referenced),
                    // so we add it manually before.
                    addIfNotNull(element.thisReceiver?.symbol)
                }
            }
            is IrScript -> {
                context.enterNewScopeForOwner(element) {
                    // By default, `thisReceiver` is always visited _after_ the script statements (where it may be referenced),
                    // so we add it manually before.
                    addIfNotNull(element.thisReceiver?.symbol)
                    addAll(element.implicitReceiversParameters.map { it.symbol })
                    addAll(element.explicitCallParameters.map { it.symbol })
                }
            }
            is IrReplSnippet -> {
                context.enterNewScopeForOwner(element) {
                    element.variablesFromOtherSnippets.mapTo(this, IrVariable::symbol)
                }
            }
            is IrFunction -> {
                context.enterNewScopeForOwner(element) {
                    // A function parameter's default value may reference the parameters that come after it,
                    // so we add all the parameters to the scope manually before validating any of them
                    element.parameters.mapTo(this, IrValueParameter::symbol)
                }
            }
            is IrAnonymousInitializer -> {
                context.enterNewScopeForOwner(element) {
                    addValueParametersOfPrimaryConstructor(element)
                }
            }
            is IrField -> {
                context.enterNewScopeForOwner(element) {
                    addValueParametersOfPrimaryConstructor(element)
                }
            }
            is IrCatch -> {
                // catchParameter only has scope over result expression, so create a new scope
                context.enterNewScopeForOwner(element)
            }
            is IrBlock -> {
                // Entering a new scope
                context.enterNewScopeForOwner(element)
            }
        }
    }

    override fun exitContext(context: CheckerContext, element: IrElement) {
        when (element) {
            is IrClass -> context.exitScopeForOwner(element)
            is IrScript -> context.exitScopeForOwner(element)
            is IrReplSnippet -> context.exitScopeForOwner(element)
            is IrFunction -> context.exitScopeForOwner(element)
            is IrAnonymousInitializer -> context.exitScopeForOwner(element)
            is IrField -> context.exitScopeForOwner(element)
            is IrCatch -> context.exitScopeForOwner(element)
            is IrBlock -> context.exitScopeForOwner(element)
        }
    }

    private fun MutableSet<IrValueSymbol>.addValueParametersOfPrimaryConstructor(declaration: IrDeclaration) {
        (declaration.parent as? IrClass)?.primaryConstructor?.parameters?.mapTo(this, IrValueParameter::symbol)
    }
}
