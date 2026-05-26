/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

private object DirectClassInheritorsKey : FirDeclarationDataKey()

private var FirRegularClass.directInheritorsAttr: Lazy<Set<ClassId>>? by FirDeclarationDataRegistry.data(DirectClassInheritorsKey)

private val FirRegularClassSymbol.directInheritorsAttr: Lazy<Set<ClassId>>? by FirDeclarationDataRegistry.symbolAccessor(DirectClassInheritorsKey)

fun FirRegularClass.setDirectInheritors(inheritors: Set<ClassId>) {
    directInheritorsAttr = lazyOf(inheritors)
}

val FirRegularClass.directInheritors: Set<ClassId> get() = directInheritorsAttr?.value ?: emptySet()

val FirRegularClassSymbol.directInheritors: Set<ClassId> get() = directInheritorsAttr?.value ?: emptySet()
