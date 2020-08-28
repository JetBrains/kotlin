/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId


sealed class KtClassLikeSymbol : KtSymbol, KtNamedSymbol, KtSymbolWithKind {
    abstract val classIdIfNonLocal: ClassId?

    abstract override fun createPointer(): KtSymbolPointer<KtClassLikeSymbol>
}

abstract class KtTypeAliasSymbol : KtClassLikeSymbol() {
    abstract override val classIdIfNonLocal: ClassId

    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.TOP_LEVEL

    abstract override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol>
}

abstract class KtClassOrObjectSymbol : KtClassLikeSymbol(),
    KtSymbolWithTypeParameters,
    KtSymbolWithModality<KtSymbolModality> {
    abstract val classKind: KtClassKind

    abstract override fun createPointer(): KtSymbolPointer<KtClassOrObjectSymbol>
}

enum class KtClassKind {
    CLASS, ENUM_CLASS, ENUM_ENTRY, ANNOTATION_CLASS, OBJECT, COMPANION_OBJECT, INTERFACE
}