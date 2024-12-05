/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.context

import org.jetbrains.kotlin.backend.common.InlineFunctionUseSiteChecker
import org.jetbrains.kotlin.backend.common.ReportIrValidationError
import org.jetbrains.kotlin.backend.common.ScopeStack
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol

internal class CheckerContext(
    val irBuiltIns: IrBuiltIns,
    val checkInlineFunctionUseSites: InlineFunctionUseSiteChecker?,
    val file: IrFile,
    private val reportError: ReportIrValidationError,
) {
    val parentChain: MutableList<IrElement> = mutableListOf()
    val typeParameterScopeStack = ScopeStack<IrTypeParameterSymbol>()
    val valueSymbolScopeStack = ScopeStack<IrValueSymbol>()

    fun error(element: IrElement, message: String) = reportError(file, element, message, parentChain)

    fun withTypeParametersInScope(container: IrTypeParametersContainer, block: () -> Unit) {
        typeParameterScopeStack.withNewScope(
            outerScopesAreInvisible = container is IrClass && !container.isInner && container.visibility != DescriptorVisibilities.LOCAL,
            populateScope = { container.typeParameters.forEach { add(it.symbol) } },
            block = block,
        )
    }

    fun withScopeOwner(owner: IrElement, block: () -> Unit, populateScope: MutableSet<IrValueSymbol>.() -> Unit = {}) {
        valueSymbolScopeStack.withNewScope(
            isGlobalScope = owner is IrScript,
            outerScopesAreInvisible = owner is IrClass && !owner.isInner && owner.visibility != DescriptorVisibilities.LOCAL,
            block = block,
            populateScope = populateScope
        )
    }
}