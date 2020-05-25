/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.builder.AbstractFirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirFileImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSealedClassImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import java.lang.IllegalStateException

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

inline val FirRegularClass.isInner get() = status.isInner
inline val FirRegularClass.isCompanion get() = status.isCompanion
inline val FirRegularClass.isData get() = status.isData
inline val FirRegularClass.isInline get() = status.isInline
inline val FirMemberDeclaration.modality get() = status.modality
inline val FirMemberDeclaration.visibility get() = status.visibility
inline val FirMemberDeclaration.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.INVISIBLE_FAKE
inline val FirMemberDeclaration.effectiveVisibility get() = status.effectiveVisibility
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

inline val FirPropertyAccessor.modality get() = status.modality
inline val FirPropertyAccessor.visibility get() = status.visibility
inline val FirPropertyAccessor.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.INVISIBLE_FAKE

inline val FirRegularClass.isLocal get() = symbol.classId.isLocal

fun AbstractFirRegularClassBuilder.addDeclaration(declaration: FirDeclaration) {
    declarations += declaration
    if (companionObject == null && declaration is FirRegularClass && declaration.isCompanion) {
        companionObject = declaration
    }
}

fun AbstractFirRegularClassBuilder.addDeclarations(declarations: Collection<FirDeclaration>) {
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
        is FirClassImpl -> declarations += declaration
        is FirSealedClassImpl -> declarations += declaration
        else -> throw IllegalStateException()
    }
}

private object IsFromVarargKey: FirDeclarationDataKey()
var FirProperty.isFromVararg: Boolean? by FirDeclarationDataRegistry.data(IsFromVarargKey)
