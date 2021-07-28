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

public sealed class KtClassifierSymbol : KtSymbol, KtPossiblyNamedSymbol

public val KtClassifierSymbol.nameOrAnonymous: Name
    get() = name ?: SpecialNames.ANONYMOUS

public abstract class KtTypeParameterSymbol : KtClassifierSymbol(), KtNamedSymbol {
    abstract override fun createPointer(): KtSymbolPointer<KtTypeParameterSymbol>

    public abstract val upperBounds: List<KtType>
    public abstract val variance: Variance
    public abstract val isReified: Boolean
}

public sealed class KtClassLikeSymbol : KtClassifierSymbol(), KtSymbolWithKind {
    public abstract val classIdIfNonLocal: ClassId?

    abstract override fun createPointer(): KtSymbolPointer<KtClassLikeSymbol>
}

public abstract class KtTypeAliasSymbol : KtClassLikeSymbol(), KtNamedSymbol {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.TOP_LEVEL

    /**
     * Returns type from right-hand site of type alias
     * If type alias has type parameters, then those type parameters will be present in result type
     */
    public abstract val expandedType: KtType

    abstract override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol>
}

public sealed class KtClassOrObjectSymbol : KtClassLikeSymbol(),
    KtAnnotatedSymbol,
    KtSymbolWithMembers {

    public abstract val classKind: KtClassKind
    public abstract val superTypes: List<KtTypeAndAnnotations>

    abstract override fun createPointer(): KtSymbolPointer<KtClassOrObjectSymbol>
}

public abstract class KtAnonymousObjectSymbol : KtClassOrObjectSymbol() {
    final override val classKind: KtClassKind get() = KtClassKind.ANONYMOUS_OBJECT
    final override val classIdIfNonLocal: ClassId? get() = null
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val name: Name? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtAnonymousObjectSymbol>
}

public abstract class KtNamedClassOrObjectSymbol : KtClassOrObjectSymbol(),
    KtSymbolWithTypeParameters,
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtNamedSymbol {

    public abstract val isInner: Boolean
    public abstract val isData: Boolean
    public abstract val isInline: Boolean
    public abstract val isFun: Boolean

    public abstract val isExternal: Boolean

    public abstract val companionObject: KtNamedClassOrObjectSymbol?

    abstract override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol>
}

public enum class KtClassKind {
    CLASS,
    ENUM_CLASS,
    ENUM_ENTRY,
    ANNOTATION_CLASS,
    OBJECT,
    COMPANION_OBJECT,
    INTERFACE,
    ANONYMOUS_OBJECT;

    public val isObject: Boolean get() = this == OBJECT || this == COMPANION_OBJECT || this == ANONYMOUS_OBJECT
}
