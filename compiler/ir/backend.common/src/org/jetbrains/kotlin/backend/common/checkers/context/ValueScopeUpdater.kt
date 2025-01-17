/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.context

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addIfNotNull

internal object ValueScopeUpdater : ContextUpdater {
    override fun runInNewContext(
        context: CheckerContext,
        element: IrElement,
        block: () -> Unit,
    ) {
        element.acceptVoid(ValueScopeVisitor(context, block))
    }

    private class ValueScopeVisitor(
        private val context: CheckerContext,
        private val block: () -> Unit
    ) : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            block()
        }

        override fun visitDeclaration(declaration: IrDeclarationBase) {
            if (declaration is IrValueDeclaration) {
                context.valueSymbolScopeStack.addToCurrentScope(declaration.symbol)
            }
            block()
        }

        override fun visitClass(declaration: IrClass) {
            context.withScopeOwner(declaration, block) {
                // By default, `thisReceiver` is always visited _after_ the child declarations (where it may be referenced),
                // so we add it manually before.
                addIfNotNull(declaration.thisReceiver?.symbol)
            }
        }

        override fun visitScript(declaration: IrScript) {
            context.withScopeOwner(declaration, block) {
                // By default, `thisReceiver` is always visited _after_ the script statements (where it may be referenced),
                // so we add it manually before.
                addIfNotNull(declaration.thisReceiver?.symbol)
            }
        }

        override fun visitFunction(declaration: IrFunction) {
            context.withScopeOwner(declaration, block) {
                // A function parameter's default value may reference the parameters that come after it,
                // so we add all the parameters to the scope manually before validating any of them
                declaration.parameters.mapTo(this, IrValueParameter::symbol)
            }
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
            context.withScopeOwner(declaration, block) {
                addValueParametersOfPrimaryConstructor(declaration)
            }
        }

        override fun visitField(declaration: IrField) {
            context.withScopeOwner(declaration, block) {
                addValueParametersOfPrimaryConstructor(declaration)
            }
        }

        override fun visitCatch(aCatch: IrCatch) {
            // catchParameter only has scope over result expression, so create a new scope
            context.withScopeOwner(aCatch, block)
        }

        override fun visitBlock(expression: IrBlock) {
            // Entering a new scope
            context.withScopeOwner(expression, block)
        }

        private fun MutableSet<IrValueSymbol>.addValueParametersOfPrimaryConstructor(declaration: IrDeclaration) {
            (declaration.parent as? IrClass)?.primaryConstructor?.parameters?.mapTo(this, IrValueParameter::symbol)
        }
    }
}