/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.declarations.utils.isOverridable
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.utils.SmartSet

/**
 * Direct Callable Overrides
 */

private object DirectCallableOverrides : FirDeclarationDataKey()

private var FirCallableDeclaration.directOverridesAttr: Lazy<SmartSet<FirCallableSymbol<*>>>? by FirDeclarationDataRegistry.data(DirectCallableOverrides)

private val FirCallableSymbol<*>.directOverridesAttr: Lazy<SmartSet<FirFunctionSymbol<*>>>? by FirDeclarationDataRegistry.symbolAccessor(DirectCallableOverrides)

fun FirCallableDeclaration.addDirectOverrides(vararg overrides: FirCallableSymbol<*>) {
    require(isOverridable)
    directOverridesAttr = lazyOf(directOverridesAttr?.value?.plus(overrides) ?: SmartSet.create<FirCallableSymbol<*>>().plus(overrides))
}

fun FirCallableDeclaration.addDirectOverrides(overrides: Set<FirCallableSymbol<*>>) {
    directOverridesAttr = lazyOf(directOverridesAttr?.value?.plus(overrides) ?: SmartSet.create<FirCallableSymbol<*>>().plus(overrides))
}

val FirCallableDeclaration.directOverrides: Set<FirCallableSymbol<*>> get() = directOverridesAttr?.value ?: emptySet()

val FirCallableSymbol<*>.directOverrides: Set<FirCallableSymbol<*>> get() = directOverridesAttr?.value ?: emptySet()

private fun <T, C : MutableCollection<T>> C.plus(others: Array<out T>): C = apply { others.forEach(::add) }

private fun <T, C : MutableCollection<T>> C.plus(others: Collection<T>): C = apply { addAll(others) }
