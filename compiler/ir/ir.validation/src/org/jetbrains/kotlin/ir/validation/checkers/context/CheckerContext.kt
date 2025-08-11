/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.context

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.validation.IrValidationError
import org.jetbrains.kotlin.ir.validation.ScopeStack
import org.jetbrains.kotlin.ir.validation.checkers.IrChecker

class CheckerContext(
    val irBuiltIns: IrBuiltIns,
    val file: IrFile,
    private val reportError: (IrValidationError) -> Unit,
) {
    val parentChain: MutableList<IrElement> = mutableListOf()
    val typeParameterScopeStack = ScopeStack<IrTypeParameterSymbol>()
    val valueSymbolScopeStack = ScopeStack<IrValueSymbol>()

    var withinAnnotationUsageSubTree: Boolean = false
        private set

    // Some checks are (temporarily) disabled for scriping related IR, because it happens to violate many of the rules of IR.
    //  At the same time:
    //  1. Kotlin scripting is supported only in JVM - so the possibly invalid IR won't be stored anywhere, as in the case of Klibs.
    //  2. The compiled code is executed immediately - so even if the generated code is invalid, you'll see the result right away.
    //  3. It somehow worked before those checks were added.
    //  This means the severity of invalid IR inside scripts is not that critical.
    var withinScripOrScriptClass: Boolean = false
        private set

    fun error(element: IrElement, cause: IrValidationError.Cause, message: String) =
        reportError(IrValidationError(file, element, cause, message, parentChain))

    context(checker: IrChecker)
    fun error(element: IrElement, message: String) = error(element, checker, message)

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

    fun withinAnnotationUsageSubTree(block: () -> Unit) {
        if (withinAnnotationUsageSubTree) {
            block()
        } else {
            withinAnnotationUsageSubTree = true
            block()
            withinAnnotationUsageSubTree = false
        }
    }

    fun withinScripOrScriptClass(block: () -> Unit) {
        if (withinScripOrScriptClass) {
            block()
        } else {
            withinScripOrScriptClass = true
            block()
            withinScripOrScriptClass = false
        }
    }
}