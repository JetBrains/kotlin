/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.context

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addIfNotNull

internal object ValueScopeUpdater : ContextUpdater {
    override fun onEnterElement(context: CheckerContext, element: IrElement) {
        when (element) {
            is IrValueDeclaration -> context.valueSymbolScopeStack.addToCurrentScope(element.symbol)
            is IrScript -> context.valueSymbolScopeStack.pushScope(isGlobalScope = true) {
                addIfNotNull(element.thisReceiver?.symbol)
            }
            is IrClass -> context.valueSymbolScopeStack.pushScope(outerScopesAreInvisible = !element.isInner && element.visibility != DescriptorVisibilities.LOCAL) {
                addIfNotNull(element.thisReceiver?.symbol)
            }
            is IrFunction -> context.valueSymbolScopeStack.pushScope {
                element.parameters.mapTo(this) { it.symbol }
            }
            is IrAnonymousInitializer, is IrField -> context.valueSymbolScopeStack.pushScope {
                element.parentClassOrNull?.primaryConstructor?.parameters?.mapTo(this, IrValueParameter::symbol)
            }
            is IrCatch, is IrBlock -> context.valueSymbolScopeStack.pushScope {}
        }
    }

    override fun onExitElement(context: CheckerContext, element: IrElement) {
        when (element) {
            is IrValueDeclaration -> {
                // keep in scope
            }
            is IrScript,
            is IrClass,
            is IrFunction,
            is IrAnonymousInitializer, is IrField,
            is IrCatch, is IrBlock
                -> context.valueSymbolScopeStack.popScope()
        }
    }
}