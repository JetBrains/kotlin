/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId

val FirTypeAlias.expandedConeType: ConeClassLikeType? get() = expandedTypeRef.coneTypeSafe()

val FirClassLikeDeclaration.classId
    get() = when (this) {
        is FirClass -> symbol.classId
        is FirTypeAlias -> symbol.classId
    }

val FirClass.classId: ClassId get() = symbol.classId

val FirClassSymbol<*>.superConeTypes: List<ConeClassLikeType>
    get() = when (this) {
        is FirRegularClassSymbol -> fir.superConeTypes
        is FirAnonymousObjectSymbol -> fir.superConeTypes
    }

val FirClass.superConeTypes: List<ConeClassLikeType> get() = superTypeRefs.mapNotNull { it.coneTypeSafe() }

val FirClass.anonymousInitializers: List<FirAnonymousInitializer>
    get() = declarations.filterIsInstance<FirAnonymousInitializer>()

val FirClass.constructors: List<FirConstructor>
    get() = declarations.filterIsInstance<FirConstructor>()

val FirConstructor.delegatedThisConstructor: FirConstructor?
    get() = delegatedConstructor?.takeIf { it.isThis }
        ?.let { (it.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirConstructor }

val FirClass.constructorsSortedByDelegation: List<FirConstructor>
    get() = constructors.sortedWith(ConstructorDelegationComparator)

val FirClass.primaryConstructor: FirConstructor?
    get() = constructors.find(FirConstructor::isPrimary)

fun FirRegularClass.collectEnumEntries(): Collection<FirEnumEntry> {
    assert(classKind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<FirEnumEntry>()
}

val FirQualifiedAccess.referredPropertySymbol: FirPropertySymbol?
    get() {
        val reference = calleeReference as? FirResolvedNamedReference ?: return null
        return reference.resolvedSymbol as? FirPropertySymbol
    }

inline val FirDeclaration.isJava: Boolean
    get() = origin == FirDeclarationOrigin.Java
inline val FirDeclaration.isFromLibrary: Boolean
    get() = origin == FirDeclarationOrigin.Library
inline val FirDeclaration.isSynthetic: Boolean
    get() = origin == FirDeclarationOrigin.Synthetic

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

