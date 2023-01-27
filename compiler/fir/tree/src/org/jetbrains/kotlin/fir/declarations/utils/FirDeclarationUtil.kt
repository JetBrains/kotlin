/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

val FirTypeAlias.expandedConeType: ConeClassLikeType? get() = expandedTypeRef.coneTypeSafe()

val FirClassLikeDeclaration.classId: ClassId
    get() = symbol.classId

val FirClass.superConeTypes: List<ConeClassLikeType> get() = superTypeRefs.mapNotNull { it.coneTypeSafe() }

val FirClass.anonymousInitializers: List<FirAnonymousInitializer>
    get() = declarations.filterIsInstance<FirAnonymousInitializer>()

val FirClass.delegateFields: List<FirField>
    get() = declarations.filterIsInstance<FirField>().filter { it.isSynthetic }

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

// NB: This function checks transitive localness. That is,
// if a declaration `isNonLocal`, then its parent also `isNonLocal`.
val FirDeclaration.isNonLocal
    get() = when (this) {
        is FirFile -> true
        is FirCallableDeclaration -> !symbol.callableId.isLocal
        is FirClassLikeDeclaration -> !symbol.classId.isLocal
        else -> false
    }

val FirCallableDeclaration.isExtension get() = receiverParameter != null

val FirMemberDeclaration.name: Name
    get() = when (this) {
        is FirCallableDeclaration ->
            this.symbol.callableId.callableName
        is FirClass ->
            this.classId.shortClassName
        is FirTypeAlias ->
            this.name
    }
