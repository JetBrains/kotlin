/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirFileImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirRegularClassImpl
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

fun FirTypeParameterBuilder.addDefaultBoundIfNecessary(isFlexible: Boolean = false) {
    if (bounds.isEmpty()) {
        val type = if (isFlexible) {
            val session = this.session
            buildResolvedTypeRef {
                type = ConeFlexibleType(session.builtinTypes.anyType.type, session.builtinTypes.nullableAnyType.type)
            }
        } else {
            session.builtinTypes.nullableAnyType
        }
        bounds += type
    }
}

inline val FirRegularClass.isInterface: Boolean
    get() = classKind == ClassKind.INTERFACE

inline val FirRegularClass.isEnumClass: Boolean
    get() = classKind == ClassKind.ENUM_CLASS

inline val FirRegularClass.modality get() = status.modality
inline val FirRegularClass.isSealed get() = status.modality == Modality.SEALED
inline val FirRegularClass.isAbstract get() = status.modality == Modality.ABSTRACT

inline val FirRegularClass.canHaveAbstractDeclaration: Boolean
    get() = isAbstract || isSealed || isEnumClass

inline val FirRegularClass.isInner get() = status.isInner
inline val FirRegularClass.isCompanion get() = status.isCompanion
inline val FirRegularClass.isData get() = status.isData
inline val FirRegularClass.isInline get() = status.isInline
inline val FirRegularClass.isFun get() = status.isFun

inline val FirMemberDeclaration.modality get() = status.modality
inline val FirMemberDeclaration.isAbstract get() = status.modality == Modality.ABSTRACT
inline val FirMemberDeclaration.isOpen get() = status.modality == Modality.OPEN

inline val FirMemberDeclaration.visibility get() = status.visibility
inline val FirMemberDeclaration.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake

inline val FirMemberDeclaration.isActual get() = status.isActual
inline val FirMemberDeclaration.isExpect get() = status.isExpect
inline val FirMemberDeclaration.isInner get() = status.isInner
inline val FirMemberDeclaration.isStatic get() = status.isStatic
inline val FirMemberDeclaration.isOverride: Boolean get() = status.isOverride
inline val FirMemberDeclaration.isOperator: Boolean get() = status.isOperator
inline val FirMemberDeclaration.isInfix: Boolean get() = status.isInfix
inline val FirMemberDeclaration.isInline: Boolean get() = status.isInline
inline val FirMemberDeclaration.isTailRec: Boolean get() = status.isTailRec
inline val FirMemberDeclaration.isExternal: Boolean get() = status.isExternal
inline val FirMemberDeclaration.isSuspend: Boolean get() = status.isSuspend
inline val FirMemberDeclaration.isConst: Boolean get() = status.isConst
inline val FirMemberDeclaration.isLateInit: Boolean get() = status.isLateInit
inline val FirMemberDeclaration.isFromSealedClass: Boolean get() = status.isFromSealedClass
inline val FirMemberDeclaration.isFromEnumClass: Boolean get() = status.isFromEnumClass

inline val FirFunction<*>.hasBody get() = body != null

inline val FirPropertyAccessor.modality get() = status.modality
inline val FirPropertyAccessor.visibility get() = status.visibility
inline val FirPropertyAccessor.isInline get() = status.isInline
inline val FirPropertyAccessor.isExternal get() = status.isExternal
inline val FirPropertyAccessor.hasBody get() = body != null

inline val FirProperty.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake
inline val FirPropertyAccessor.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake

inline val FirRegularClass.isLocal get() = symbol.classId.isLocal
inline val FirSimpleFunction.isLocal get() = status.visibility == Visibilities.Local

fun FirRegularClassBuilder.addDeclaration(declaration: FirDeclaration) {
    declarations += declaration
    if (companionObject == null && declaration is FirRegularClass && declaration.isCompanion) {
        companionObject = declaration
    }
}

fun FirRegularClassBuilder.addDeclarations(declarations: Collection<FirDeclaration>) {
    declarations.forEach(this::addDeclaration)
}

val FirTypeAlias.expandedConeType: ConeClassLikeType? get() = expandedTypeRef.coneTypeSafe()

val FirClass<*>.classId get() = symbol.classId

val FirClassSymbol<*>.superConeTypes
    get() = when (this) {
        is FirRegularClassSymbol -> fir.superConeTypes
        is FirAnonymousObjectSymbol -> fir.superConeTypes
    }

val FirClass<*>.superConeTypes get() = superTypeRefs.mapNotNull { it.coneTypeSafe<ConeClassLikeType>() }

val FirClass<*>.anonymousInitializers: List<FirAnonymousInitializer>
    get() = declarations.filterIsInstance<FirAnonymousInitializer>()

val FirClass<*>.constructors: List<FirConstructor>
    get() = declarations.filterIsInstance<FirConstructor>()

val FirConstructor.delegatedThisConstructor: FirConstructor?
    get() = delegatedConstructor?.takeIf { it.isThis }
        ?.let { (it.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirConstructor }

private object ConstructorDelegationComparator : Comparator<FirConstructor> {
    override fun compare(p0: FirConstructor?, p1: FirConstructor?): Int {
        if (p0 == null && p1 == null) return 0
        if (p0 == null) return -1
        if (p1 == null) return 1
        if (p0.delegatedThisConstructor == p1) return 1
        if (p1.delegatedThisConstructor == p0) return -1
        // If neither is a delegation to each other, the order doesn't matter.
        // Here we return 0 to preserve the original order.
        return 0
    }
}

val FirClass<*>.constructorsSortedByDelegation: List<FirConstructor>
    get() = constructors.sortedWith(ConstructorDelegationComparator)

val FirClass<*>.primaryConstructor: FirConstructor?
    get() = constructors.firstOrNull()?.takeIf { it.isPrimary }

fun FirRegularClass.collectEnumEntries(): Collection<FirEnumEntry> {
    assert(classKind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<FirEnumEntry>()
}

fun FirFile.addDeclaration(declaration: FirDeclaration) {
    require(this is FirFileImpl)
    declarations += declaration
}

fun FirRegularClass.addDeclaration(declaration: FirDeclaration) {
    @Suppress("LiftReturnOrAssignment")
    when (this) {
        is FirRegularClassImpl -> declarations += declaration
        else -> throw IllegalStateException()
    }
}

private object SourceElementKey : FirDeclarationDataKey()

var FirRegularClass.sourceElement: SourceElement? by FirDeclarationDataRegistry.data(SourceElementKey)

var FirTypeAlias.sourceElement: SourceElement? by FirDeclarationDataRegistry.data(SourceElementKey)

val FirMemberDeclaration.containerSource: SourceElement?
    get() = when (this) {
        is FirCallableMemberDeclaration<*> -> containerSource
        is FirRegularClass -> sourceElement
        is FirTypeAlias -> sourceElement
        else -> null
    }

private object IsFromVarargKey : FirDeclarationDataKey()

private object IsReferredViaField : FirDeclarationDataKey()

var FirProperty.isFromVararg: Boolean? by FirDeclarationDataRegistry.data(IsFromVarargKey)
var FirProperty.isReferredViaField: Boolean? by FirDeclarationDataRegistry.data(IsReferredViaField)

// See [BindingContext.BACKING_FIELD_REQUIRED]
val FirProperty.hasBackingField: Boolean
    get() {
        if (isAbstract) return false
        if (delegate != null) return false
        when (origin) {
            FirDeclarationOrigin.SubstitutionOverride -> return false
            FirDeclarationOrigin.IntersectionOverride -> return false
            FirDeclarationOrigin.Delegated -> return false
            else -> {
                val getter = getter ?: return true
                if (isVar && setter == null) return true
                if (setter?.hasBody == false && setter?.isAbstract == false) return true
                if (!getter.hasBody && !getter.isAbstract) return true

                return isReferredViaField == true
            }
        }
    }

inline val FirDeclaration.isFromLibrary: Boolean
    get() = origin == FirDeclarationOrigin.Library
inline val FirDeclaration.isSynthetic: Boolean
    get() = origin == FirDeclarationOrigin.Synthetic
