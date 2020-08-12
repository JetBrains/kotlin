/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * [KtSymbol] which can possibly be class member, top level or local declaration
 * Contracts:
 *   [containingNonLocalClassIdIfMember] != null iff [symbolKind] == [KtSymbolKind.MEMBER]
 *   [containingPackageFqNameIfTopLevel] != null iff [symbolKind] == [KtSymbolKind.TOP_LEVEL]
 */
interface KtSymbolWithKind : KtSymbol {
    val symbolKind: KtSymbolKind
    val containingNonLocalClassIdIfMember: ClassId?
    val containingPackageFqNameIfTopLevel: FqName?
}

enum class KtSymbolKind {
    TOP_LEVEL, MEMBER, LOCAL, NON_PROPERTY_PARAMETER
}

val KtSymbol.containingClassIdIfMember: ClassId?
    get() = (this as? KtSymbolWithKind)?.containingClassIdIfMember

val KtSymbol.containingPackageFqNameIfTopLevel: FqName?
    get() = (this as? KtSymbolWithKind)?.containingPackageFqNameIfTopLevel

