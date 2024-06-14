/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

public sealed class KaClassifierSymbol : KaSymbol, KaPossiblyNamedSymbol, KaDeclarationSymbol

@Deprecated("Use 'KaClassifierSymbol' instead", ReplaceWith("KaClassifierSymbol"))
public typealias KtClassifierSymbol = KaClassifierSymbol

public val KaClassifierSymbol.nameOrAnonymous: Name
    get() = name ?: SpecialNames.ANONYMOUS

public abstract class KaTypeParameterSymbol : KaClassifierSymbol(), KaNamedSymbol {
    abstract override fun createPointer(): KaSymbolPointer<KaTypeParameterSymbol>

    final override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    public abstract val upperBounds: List<KaType>
    public abstract val variance: Variance
    public abstract val isReified: Boolean
}

@Deprecated("Use 'KaTypeParameterSymbol' instead", ReplaceWith("KaTypeParameterSymbol"))
public typealias KtTypeParameterSymbol = KaTypeParameterSymbol

public sealed class KaClassLikeSymbol : KaClassifierSymbol(), @Suppress("DEPRECATION") KaSymbolWithKind, KaPossibleMemberSymbol,
    KaPossibleMultiplatformSymbol {
    /**
     * The [ClassId] of this class, or `null` if this class is local.
     */
    public abstract val classId: ClassId?

    @Deprecated("Use `classId` instead.", ReplaceWith("classId"))
    public val classIdIfNonLocal: ClassId? get() = classId

    abstract override fun createPointer(): KaSymbolPointer<KaClassLikeSymbol>
}

@Deprecated("Use 'KaClassLikeSymbol' instead", ReplaceWith("KaClassLikeSymbol"))
public typealias KtClassLikeSymbol = KaClassLikeSymbol

public abstract class KaTypeAliasSymbol : KaClassLikeSymbol(),
    KaSymbolWithVisibility,
    KaNamedSymbol {

    /**
     * Returns type from right-hand site of type alias
     * If type alias has type parameters, then those type parameters will be present in result type
     */
    public abstract val expandedType: KaType

    abstract override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol>
}

@Deprecated("Use 'KaTypeAliasSymbol' instead", ReplaceWith("KaTypeAliasSymbol"))
public typealias KtTypeAliasSymbol = KaTypeAliasSymbol

public sealed class KaClassOrObjectSymbol : KaClassLikeSymbol(), KaSymbolWithMembers {

    public abstract val classKind: KaClassKind
    public abstract val superTypes: List<KaType>

    abstract override fun createPointer(): KaSymbolPointer<KaClassOrObjectSymbol>
}

@Deprecated("Use 'KaClassOrObjectSymbol' instead", ReplaceWith("KaClassOrObjectSymbol"))
public typealias KtClassOrObjectSymbol = KaClassOrObjectSymbol

public abstract class KaAnonymousObjectSymbol : KaClassOrObjectSymbol() {
    final override val classKind: KaClassKind get() = withValidityAssertion { KaClassKind.ANONYMOUS_OBJECT }
    final override val classId: ClassId? get() = withValidityAssertion { null }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.LOCAL }
    final override val name: Name? get() = withValidityAssertion { null }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    final override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    abstract override fun createPointer(): KaSymbolPointer<KaAnonymousObjectSymbol>
}

@Deprecated("Use 'KaAnonymousObjectSymbol' instead", ReplaceWith("KaAnonymousObjectSymbol"))
public typealias KtAnonymousObjectSymbol = KaAnonymousObjectSymbol

public abstract class KaNamedClassOrObjectSymbol : KaClassOrObjectSymbol(),
    KaSymbolWithModality,
    KaSymbolWithVisibility,
    KaNamedSymbol,
    KaContextReceiversOwner {

    public abstract val isInner: Boolean
    public abstract val isData: Boolean
    public abstract val isInline: Boolean
    public abstract val isFun: Boolean

    public abstract val isExternal: Boolean

    public abstract val companionObject: KaNamedClassOrObjectSymbol?

    abstract override fun createPointer(): KaSymbolPointer<KaNamedClassOrObjectSymbol>
}

@Deprecated("Use 'KaNamedClassOrObjectSymbol' instead", ReplaceWith("KaNamedClassOrObjectSymbol"))
public typealias KtNamedClassOrObjectSymbol = KaNamedClassOrObjectSymbol

public enum class KaClassKind {
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

@Deprecated("Use 'KaClassKind' instead", ReplaceWith("KaClassKind"))
public typealias KtClassKind = KaClassKind