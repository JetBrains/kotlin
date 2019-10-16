/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirEnumEntryImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

fun FirTypeParameterImpl.addDefaultBoundIfNecessary() {
    if (bounds.isEmpty()) {
        bounds += session.builtinTypes.nullableAnyType
    }
}

inline val FirRegularClass.isInner get() = status.isInner
inline val FirRegularClass.isCompanion get() = status.isCompanion
inline val FirRegularClass.isData get() = status.isData
inline val FirRegularClass.isInline get() = status.isInline
inline val FirMemberDeclaration.modality get() = status.modality
inline val FirMemberDeclaration.visibility get() = status.visibility
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

inline val FirPropertyAccessor.modality get() = status.modality
inline val FirPropertyAccessor.visibility get() = status.visibility

fun FirClassImpl.addDeclaration(declaration: FirDeclaration) {
    declarations += declaration
    if (companionObject == null && declaration is FirRegularClass && declaration.isCompanion) {
        companionObject = declaration
    }
}

fun FirClassImpl.addDeclarations(declarations: Collection<FirDeclaration>) {
    declarations.forEach(this::addDeclaration)
}

fun FirEnumEntryImpl.addDeclaration(declaration: FirDeclaration) {
    declarations += declaration
}

val FirTypeAlias.expandedConeType: ConeClassLikeType? get() = expandedTypeRef.coneTypeSafe()

val FirRegularClass.classId get() = symbol.classId

val FirClass.superConeTypes get() = superTypeRefs.mapNotNull { it.coneTypeSafe<ConeClassLikeType>() }