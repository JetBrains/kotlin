/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
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

fun FirModifiableRegularClass.addDeclaration(declaration: FirDeclaration) {
    declarations += declaration
    if (companionObject == null && declaration is FirRegularClass && declaration.isCompanion) {
        companionObject = declaration
    }
}

fun FirModifiableRegularClass.addDeclarations(declarations: Collection<FirDeclaration>) {
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

fun FirRegularClass.collectEnumEntries(): Collection<FirPropertySymbol> {
    assert(classKind == ClassKind.ENUM_CLASS)
    return declarations
        .mapNotNull {
            if (it !is FirProperty) return@mapNotNull null
            val initializer = it.initializer
            if (initializer == null || initializer !is FirAnonymousObject || initializer.classKind != ClassKind.ENUM_ENTRY)
                return@mapNotNull null
            return@mapNotNull it.symbol
        }
}