/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

public sealed class KtClassifierSymbol : KtSymbol, KtPossiblyNamedSymbol, KtDeclarationSymbol

public val KtClassifierSymbol.nameOrAnonymous: Name
    get() = name ?: SpecialNames.ANONYMOUS

public abstract class KtTypeParameterSymbol : KtClassifierSymbol(), KtNamedSymbol {
    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtTypeParameterSymbol>

    final override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    public abstract val upperBounds: List<KtType>
    public abstract val variance: Variance
    public abstract val isReified: Boolean
}

public sealed class KtClassLikeSymbol : KtClassifierSymbol(), KtSymbolWithKind, KtPossibleMemberSymbol, KtPossibleMultiplatformSymbol {
    public abstract val classIdIfNonLocal: ClassId?

    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtClassLikeSymbol>
}

public abstract class KtTypeAliasSymbol : KtClassLikeSymbol(),
    KtSymbolWithVisibility,
    KtNamedSymbol {

    /**
     * Returns type from right-hand site of type alias
     * If type alias has type parameters, then those type parameters will be present in result type
     */
    public abstract val expandedType: KtType

    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtTypeAliasSymbol>
}

public sealed class KtClassOrObjectSymbol : KtClassLikeSymbol(), KtSymbolWithMembers {

    public abstract val classKind: KtClassKind
    public abstract val superTypes: List<KtType>

    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtClassOrObjectSymbol>
}

public abstract class KtAnonymousObjectSymbol : KtClassOrObjectSymbol() {
    final override val classKind: KtClassKind get() = withValidityAssertion { KtClassKind.ANONYMOUS_OBJECT }
    final override val classIdIfNonLocal: ClassId? get() = withValidityAssertion { null }
    final override val symbolKind: KtSymbolKind get() = withValidityAssertion { KtSymbolKind.LOCAL }
    final override val name: Name? get() = withValidityAssertion { null }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    final override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtAnonymousObjectSymbol>
}

public abstract class KtNamedClassOrObjectSymbol : KtClassOrObjectSymbol(),
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtNamedSymbol,
    KtContextReceiversOwner {

    public abstract val isInner: Boolean
    public abstract val isData: Boolean
    public abstract val isInline: Boolean
    public abstract val isFun: Boolean

    public abstract val isExternal: Boolean

    public abstract val companionObject: KtNamedClassOrObjectSymbol?

    context(KtAnalysisSession)
    abstract override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol>
}

public enum class KtClassKind {
    CLASS,
    ENUM_CLASS,
    ANNOTATION_CLASS,
    OBJECT,
    COMPANION_OBJECT,
    INTERFACE,
    ANONYMOUS_OBJECT;

    public val isObject: Boolean get() = this == OBJECT || this == COMPANION_OBJECT || this == ANONYMOUS_OBJECT
    public val isClass: Boolean get() = this == CLASS || this == ANNOTATION_CLASS || this == ENUM_CLASS
}
