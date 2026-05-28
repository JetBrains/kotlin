/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.utils.SmartSet

private object DirectClassInheritorsKey : FirDeclarationDataKey()

private var FirRegularClass.directInheritorsAttr: Lazy<SmartSet<FirClassSymbol<*>>>? by FirDeclarationDataRegistry.data(DirectClassInheritorsKey)

private val FirRegularClassSymbol.directInheritorsAttr: Lazy<SmartSet<FirClassSymbol<*>>>? by FirDeclarationDataRegistry.symbolAccessor(DirectClassInheritorsKey)

fun FirRegularClass.addDirectInheritors(vararg inheritors: FirClassSymbol<*>) {
    directInheritorsAttr = lazyOf(directInheritorsAttr?.value?.plus(inheritors) ?: SmartSet.create<FirClassSymbol<*>>().plus(inheritors))
}

fun FirRegularClass.addDirectInheritors(inheritors: Set<FirClassSymbol<*>>) {
    directInheritorsAttr = lazyOf(directInheritorsAttr?.value?.plus(inheritors) ?: SmartSet.create<FirClassSymbol<*>>().plus(inheritors))
}

val FirRegularClass.directInheritors: Set<FirClassSymbol<*>> get() = directInheritorsAttr?.value ?: emptySet()

val FirRegularClassSymbol.directInheritors: Set<FirClassSymbol<*>> get() = directInheritorsAttr?.value ?: emptySet()

private fun <T, C : MutableCollection<T>> C.plus(others: Array<out T>): C = apply { others.forEach(this::add) }

private fun <T, C : MutableCollection<T>> C.plus(others: Collection<T>): C = apply { others.forEach(this::add) }
