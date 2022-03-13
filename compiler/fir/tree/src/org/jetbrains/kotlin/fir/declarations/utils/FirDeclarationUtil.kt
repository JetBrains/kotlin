/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
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

val FirClass.delegateFields: List<FirField>
    get() = declarations.filterIsInstance<FirField>().filter { it.isSynthetic }

val FirQualifiedAccess.referredPropertySymbol: FirPropertySymbol?
    get() {
        return calleeReference.resolvedSymbol as? FirPropertySymbol
    }

inline val FirDeclaration.isJava: Boolean
    get() = origin == FirDeclarationOrigin.Java
inline val FirDeclaration.isFromLibrary: Boolean
    get() = origin == FirDeclarationOrigin.Library
inline val FirDeclaration.isPrecompiled: Boolean
    get() = origin == FirDeclarationOrigin.Precompiled
inline val FirDeclaration.isSynthetic: Boolean
    get() = origin == FirDeclarationOrigin.Synthetic

inline val FirDeclaration.isJavaOrEnhancement: Boolean
    get() = origin == FirDeclarationOrigin.Java || origin == FirDeclarationOrigin.Enhancement
inline val FirBasedSymbol<*>.isJavaOrEnhancement: Boolean
    get() = origin == FirDeclarationOrigin.Java || origin == FirDeclarationOrigin.Enhancement

