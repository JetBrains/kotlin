/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

sealed class KtClassifierSymbol : KtSymbol, KtPossiblyNamedSymbol

val KtClassifierSymbol.nameOrAnonymous: Name
    get() = name ?: SpecialNames.ANONYMOUS_FUNCTION

abstract class KtTypeParameterSymbol : KtClassifierSymbol(), KtNamedSymbol {
    abstract override fun createPointer(): KtSymbolPointer<KtTypeParameterSymbol>

    abstract val upperBounds: List<KtType>
    abstract val variance: Variance
    abstract val isReified: Boolean
}

sealed class KtClassLikeSymbol : KtClassifierSymbol(), KtSymbolWithKind {
    abstract val classIdIfNonLocal: ClassId?

    abstract override fun createPointer(): KtSymbolPointer<KtClassLikeSymbol>
}

abstract class KtTypeAliasSymbol : KtClassLikeSymbol(), KtNamedSymbol {
    abstract override val classIdIfNonLocal: ClassId

    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.TOP_LEVEL

    abstract override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol>
}

sealed class KtClassOrObjectSymbol : KtClassLikeSymbol(),
    KtAnnotatedSymbol,
    KtSymbolWithMembers {

    abstract val classKind: KtClassKind
    abstract val superTypes: List<KtTypeAndAnnotations>

    abstract override fun createPointer(): KtSymbolPointer<KtClassOrObjectSymbol>
}

abstract class KtAnonymousObjectSymbol : KtClassOrObjectSymbol() {
    final override val classKind: KtClassKind get() = KtClassKind.ANONYMOUS_OBJECT
    final override val classIdIfNonLocal: ClassId? get() = null
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val name: Name? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtAnonymousObjectSymbol>
}

abstract class KtNamedClassOrObjectSymbol : KtClassOrObjectSymbol(),
    KtSymbolWithTypeParameters,
    KtSymbolWithModality<KtSymbolModality>,
    KtSymbolWithVisibility,
    KtNamedSymbol {

    abstract val isInner: Boolean
    abstract val isData: Boolean
    abstract val isInline: Boolean
    abstract val isFun: Boolean

    abstract val isExternal: Boolean

    abstract val companionObject: KtNamedClassOrObjectSymbol?

    abstract override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol>
}

enum class KtClassKind {
    CLASS, ENUM_CLASS, ENUM_ENTRY, ANNOTATION_CLASS, OBJECT, COMPANION_OBJECT, INTERFACE, ANONYMOUS_OBJECT
}
