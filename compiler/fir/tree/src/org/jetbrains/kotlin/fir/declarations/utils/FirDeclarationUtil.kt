/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
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

val FirClass.superConeTypes: List<ConeClassLikeType> get() = superTypeRefs.mapNotNull { it.coneTypeSafe() }

val FirClass.anonymousInitializers: List<FirAnonymousInitializer>
    get() = declarations.filterIsInstance<FirAnonymousInitializer>()

val FirClass.delegateFields: List<FirField>
    get() = declarations.filterIsInstance<FirField>().filter { it.isSynthetic }

val FirQualifiedAccess.referredVariableSymbol: FirVariableSymbol<*>?
    get() = calleeReference.resolvedSymbol as? FirVariableSymbol<*>

val FirQualifiedAccess.referredPropertySymbol: FirPropertySymbol?
    get() = referredVariableSymbol as? FirPropertySymbol

inline val FirDeclaration.isJava: Boolean
    get() = origin is FirDeclarationOrigin.Java
inline val FirDeclaration.isJavaSource: Boolean
    get() = origin == FirDeclarationOrigin.Java.Source
inline val FirDeclaration.isFromLibrary: Boolean
    get() = origin == FirDeclarationOrigin.Library || origin == FirDeclarationOrigin.Java.Library
inline val FirDeclaration.isPrecompiled: Boolean
    get() = origin == FirDeclarationOrigin.Precompiled
inline val FirDeclaration.isSynthetic: Boolean
    get() = origin == FirDeclarationOrigin.Synthetic

inline val FirDeclaration.isJavaOrEnhancement: Boolean
    get() = origin is FirDeclarationOrigin.Java || origin == FirDeclarationOrigin.Enhancement
inline val FirBasedSymbol<*>.isJavaOrEnhancement: Boolean
    get() = origin is FirDeclarationOrigin.Java || origin == FirDeclarationOrigin.Enhancement

