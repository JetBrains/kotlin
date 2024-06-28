/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Makes sure that all the variable and value parameter references are within the scope
 * of the corresponding variables and value parameters.
 */
internal class IrValueScopeValidator(
    private val reportError: ReportError,
    private val parentChain: MutableList<IrElement>
) {
    fun check(element: IrElement) {
        element.acceptVoid(Checker())
    }

    private inner class Checker : IrElementVisitorVoid {
        private val scopeStack = ScopeStack<IrValueSymbol>()

        private fun checkValueAccess(expression: IrValueAccessExpression) {
            if (!scopeStack.isVisibleInCurrentScope(expression.symbol)) {
                reportError(expression, "The following expression references a value that is not available in the current scope.")
            }
        }

        override fun visitElement(element: IrElement) {
            parentChain.temporarilyPushing(element) {
                element.acceptChildrenVoid(this)
            }
        }

        private fun visitScopeOwner(owner: IrElement, populateScope: MutableSet<IrValueSymbol>.() -> Unit = {}) {
            scopeStack.withNewScope(
                isGlobalScope = owner is IrScript,
                outerScopesAreInvisible = owner is IrClass && !owner.isInner && owner.visibility != DescriptorVisibilities.LOCAL,
                populateScope,
            ) {
                parentChain.temporarilyPushing(owner) {
                    owner.acceptChildrenVoid(this)
                }
            }
        }

        override fun visitDeclaration(declaration: IrDeclarationBase) {
            if (declaration is IrValueDeclaration) {
                scopeStack.addToCurrentScope(declaration.symbol)
            }
            super.visitDeclaration(declaration)
        }

        override fun visitClass(declaration: IrClass) {
            visitScopeOwner(declaration) {
                // By default, `thisReceiver` is always visited _after_ the child declarations (where it may be referenced),
                // so we add it manually before.
                addIfNotNull(declaration.thisReceiver?.symbol)
            }
        }

        override fun visitScript(declaration: IrScript) {
            visitScopeOwner(declaration) {
                // By default, `thisReceiver` is always visited _after_ the script statements (where it may be referenced),
                // so we add it manually before.
                addIfNotNull(declaration.thisReceiver?.symbol)
            }
        }

        override fun visitFunction(declaration: IrFunction) {
            visitScopeOwner(declaration) {
                // A function parameter's default value may reference the parameters that come after it,
                // so we add all the parameters to the scope manually before validating any of them
                declaration.valueParameters.mapTo(this, IrValueParameter::symbol)
            }
        }

        private fun MutableSet<IrValueSymbol>.addValueParametersOfPrimaryConstructor(declaration: IrDeclaration) {
            (declaration.parent as? IrClass)?.primaryConstructor?.valueParameters?.mapTo(this, IrValueParameter::symbol)
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
            visitScopeOwner(declaration) {
                addValueParametersOfPrimaryConstructor(declaration)
            }
        }

        override fun visitField(declaration: IrField) {
            visitScopeOwner(declaration) {
                addValueParametersOfPrimaryConstructor(declaration)
            }
        }

        override fun visitBlock(expression: IrBlock) {
            // Entering a new scope
            visitScopeOwner(expression)
        }

        override fun visitCatch(aCatch: IrCatch) {
            // catchParameter only has scope over result expression, so create a new scope
            visitScopeOwner(aCatch)
        }

        override fun visitValueAccess(expression: IrValueAccessExpression) {
            checkValueAccess(expression)
            super.visitValueAccess(expression)
        }
    }
}