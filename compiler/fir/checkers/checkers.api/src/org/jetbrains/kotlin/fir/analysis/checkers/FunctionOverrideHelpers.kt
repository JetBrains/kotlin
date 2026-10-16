/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenMembers
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processOverriddenProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

@OptIn(ScopeFunctionRequiresPrewarm::class)
context(context: CheckerContext)
fun FirCallableSymbol<*>.directOverriddenSymbolsSafe(): List<FirCallableSymbol<*>> {
    if (!this.isOverride) return emptyList()
    val scope = containingClassUnsubstitutedScope() ?: return emptyList()
    scope.processFunctionsByName(this.name) { }
    return scope.getDirectOverriddenMembers(this, true)
}

context(context: CheckerContext)
fun FirNamedFunctionSymbol.directOverriddenFunctionsSafe(): List<FirNamedFunctionSymbol> = directOverriddenFunctionsSafe(context)

fun FirNamedFunctionSymbol.directOverriddenFunctionsSafe(context: CheckerContext): List<FirNamedFunctionSymbol> {
    context(context) {
        @Suppress("UNCHECKED_CAST")
        return directOverriddenSymbolsSafe() as List<FirNamedFunctionSymbol>
    }
}

context(context: CheckerContext)
fun FirPropertySymbol.directOverriddenPropertiesSafe(): List<FirPropertySymbol> = directOverriddenPropertiesSafe(context)

fun FirPropertySymbol.directOverriddenPropertiesSafe(context: CheckerContext): List<FirPropertySymbol> {
    with(context) {
        @Suppress("UNCHECKED_CAST")
        return directOverriddenSymbolsSafe() as List<FirPropertySymbol>
    }
}

context(context: CheckerContext)
inline fun FirNamedFunctionSymbol.processOverriddenFunctionsSafe(
    crossinline action: (FirNamedFunctionSymbol) -> Unit,
) {
    processOverriddenFunctionsWithActionSafe {
        action(it)
        NEXT
    }
}

@OptIn(ScopeFunctionRequiresPrewarm::class)
context(context: CheckerContext)
fun FirNamedFunctionSymbol.processOverriddenFunctionsWithActionSafe(
    action: (FirNamedFunctionSymbol) -> ProcessorAction,
) {
    val firTypeScope = containingClassUnsubstitutedScope() ?: return
    firTypeScope.processFunctionsByName(callableId.callableName) { }
    firTypeScope.processOverriddenFunctions(this, action)
}

@OptIn(ScopeFunctionRequiresPrewarm::class)
context(context: CheckerContext)
fun FirPropertySymbol.processOverriddenPropertiesWithActionSafe(
    action: (FirPropertySymbol) -> ProcessorAction,
) {
    val firTypeScope = containingClassUnsubstitutedScope() ?: return
    firTypeScope.processPropertiesByName(name) { }
    firTypeScope.processOverriddenProperties(this, action)
}

context(context: CheckerContext)
private fun FirCallableSymbol<*>.containingClassUnsubstitutedScope(): FirTypeScope? {
    val containingClass = getContainingClassSymbol() as? FirClassSymbol ?: return null
    return containingClass.unsubstitutedScope()
}
