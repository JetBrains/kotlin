/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.symbol

import org.jetbrains.kotlin.backend.common.ScopeStack
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.context.ContextUpdater
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addIfNotNull

internal class IrScopeCheckerContext {
    val stack = ScopeStack<IrSymbol>()
}

sealed class SymbolVisibility {
    data object Global : SymbolVisibility()
    data class InFile(val file: IrFile) : SymbolVisibility()
    data object Local : SymbolVisibility()
}

internal class IrScopeContextUpdater(val scopeContext: IrScopeCheckerContext) : ContextUpdater {
    override fun onEnterElement(
        context: CheckerContext,
        element: IrElement,
    ) {
        fun inFunctionScope(function: IrFunction) {
            scopeContext.stack.pushScope {
                function.parameters.mapTo(this) { it.symbol }
                function.typeParameters.mapTo(this) { it.symbol }
            }
        }

        if (element is IrSymbolOwner) {
            scopeContext.stack.addToCurrentScope(element.symbol)
        }
        if (element is IrLocalDelegatedProperty) {
            // they are visible to each other, so let's add them right now
            element.getter.let { scopeContext.stack.addToCurrentScope(it.symbol) }
            element.setter?.let { scopeContext.stack.addToCurrentScope(it.symbol) }
            element.delegate.let { scopeContext.stack.addToCurrentScope(it.symbol) }
        }
        when (element) {
            is IrScript -> {
                scopeContext.stack.pushScope(isGlobalScope = true) {
                    addIfNotNull(element.thisReceiver?.symbol)
                }
            }
            is IrClass -> {
                for (declaration in element.declarations) {
                    if (declaration is IrFunction && declaration.dispatchReceiverParameter == null) {
                        scopeContext.stack.addToCurrentScope(declaration.symbol)
                    }
                }
                scopeContext.stack.pushScope(outerScopesAreInvisible = !element.isInner && element.visibility != DescriptorVisibilities.LOCAL) {
                    for (declaration in element.declarations) {
                        if (declaration !is IrFunction || declaration.dispatchReceiverParameter != null) {
                            scopeContext.stack.addToCurrentScope(declaration.symbol)
                        }
                    }
                    addIfNotNull(element.thisReceiver?.symbol)
                    element.typeParameters.mapTo(this) { it.symbol }
                }
            }
            is IrFunction -> {
                inFunctionScope(element)
            }
            is IrAnonymousInitializer, is IrField -> {
                val primaryConstructor = element.parentClassOrNull?.primaryConstructor
                if (primaryConstructor != null) {
                    inFunctionScope(primaryConstructor)
                } else {
                    scopeContext.stack.pushScope {}
                }
            }
            is IrCatch, is IrBlock, is IrBranch, is IrBody, is IrTry,
            is IrWhen, is IrLoop -> scopeContext.stack.pushScope {}
        }
    }

    override fun onExitElement(
        context: CheckerContext,
        element: IrElement,
    ) {
        when (element) {
            is IrScript,
            is IrClass,
            is IrFunction,
            is IrAnonymousInitializer, is IrField,
            is IrCatch, is IrBlock, is IrBranch, is IrBody, is IrTry,
            is IrWhen, is IrLoop -> {
                scopeContext.stack.popScope()
            }
        }
    }

}

internal class IrScopeChecker(val scopeContext: IrScopeCheckerContext) : IrSymbolChecker {

    private fun IrSymbol.getVisibility(): SymbolVisibility {
        if (!isBound) return SymbolVisibility.Global
        if (this is IrReturnableBlockSymbol) return SymbolVisibility.Local
        if (this is IrLocalDelegatedPropertySymbol) return SymbolVisibility.Local
        val owner = owner as? IrDeclaration ?: return SymbolVisibility.Global
        if (owner.isLocal) {
            return when (owner) {
                is IrClass -> SymbolVisibility.InFile(owner.file)
                is IrFunction if owner.dispatchReceiverParameter != null && owner.parent is IrClass -> SymbolVisibility.InFile(owner.file)
                is IrProperty if owner.parent is IrClass -> SymbolVisibility.InFile(owner.file)
                else -> SymbolVisibility.Local
            }
        }
        if (owner.parentDeclarationsWithSelf.any { it is IrDeclarationWithVisibility && DescriptorVisibilities.isPrivate(it.visibility) }) {
            return SymbolVisibility.InFile(owner.file)
        }
        return SymbolVisibility.Global
    }

    override fun check(symbol: IrSymbol, container: IrElement, context: CheckerContext) {
        if (container is IrSymbolOwner && symbol == container.symbol) return
        if (symbol is IrTypeParameterSymbol && symbol.owner.parent == container) return // TODO: this is strange
        when (val visibility = symbol.getVisibility()) {
            is SymbolVisibility.Global -> {}
            is SymbolVisibility.InFile -> {
                if (context.file != visibility.file) {
                    context.error(container, "Declaration ${symbol.owner.render()} is visible only file ${visibility.file}")
                }
            }
            is SymbolVisibility.Local -> {
                if (!scopeContext.stack.isVisibleInCurrentScope(symbol)) {
                    context.error(container, "Declaration ${symbol.owner.render()} is visible only in it's local scope")
                    symbol.getVisibility()
                }
            }
        }
    }
}