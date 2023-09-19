/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId

fun FirClassLikeDeclaration.getContainingDeclaration(session: FirSession): FirClassLikeDeclaration? {
    if (isLocal) {
        @OptIn(LookupTagInternals::class)
        return (this as? FirRegularClass)?.containingClassForLocalAttr?.toFirRegularClass(session)
    } else {
        val classId = symbol.classId
        val parentId = classId.relativeClassName.parent()
        if (!parentId.isRoot) {
            val containingDeclarationId = ClassId(classId.packageFqName, parentId, isLocal = false)
            return session.symbolProvider.getClassLikeSymbolByClassId(containingDeclarationId)?.fir
        }
    }

    return null
}

fun isValidTypeParameterFromOuterDeclaration(
    typeParameterSymbol: FirTypeParameterSymbol,
    declaration: FirDeclaration?,
    session: FirSession
): Boolean {
    if (declaration == null) {
        return true  // Extra check is required because of classDeclaration will be resolved later
    }

    val visited = mutableSetOf<FirDeclaration>()

    fun containsTypeParameter(currentDeclaration: FirDeclaration?): Boolean {
        if (currentDeclaration == null || !visited.add(currentDeclaration)) {
            return false
        }

        if (currentDeclaration is FirTypeParameterRefsOwner) {
            if (currentDeclaration.typeParameters.any { it.symbol == typeParameterSymbol }) {
                return true
            }

            if (currentDeclaration is FirCallableDeclaration) {
                val containingClassId = currentDeclaration.symbol.callableId.classId ?: return true
                return containsTypeParameter(session.symbolProvider.getClassLikeSymbolByClassId(containingClassId)?.fir)
            } else if (currentDeclaration is FirClass) {
                for (superTypeRef in currentDeclaration.superTypeRefs) {
                    val superClassFir = superTypeRef.firClassLike(session)
                    if (superClassFir == null || superClassFir is FirRegularClass && containsTypeParameter(superClassFir)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    return containsTypeParameter(declaration)
}

fun FirTypeRef.firClassLike(session: FirSession): FirClassLikeDeclaration? {
    val type = coneTypeSafe<ConeClassLikeType>() ?: return null
    return type.lookupTag.toSymbol(session)?.fir
}

fun List<FirQualifierPart>.toTypeProjections(): Array<ConeTypeProjection> =
    asReversed().flatMap { it.typeArgumentList.typeArguments.map { typeArgument -> typeArgument.toConeTypeProjection() } }.toTypedArray()

interface FirCodeFragmentContext {
    val towerDataContext: FirTowerDataContext
    val smartCasts: Map<RealVariable, Set<ConeKotlinType>>
}

private object CodeFragmentContextDataKey : FirDeclarationDataKey()

var FirCodeFragment.codeFragmentContext: FirCodeFragmentContext? by FirDeclarationDataRegistry.data(CodeFragmentContextDataKey)

/**
 * If `classLikeType` is an inner class,
 * then this function returns a type representing
 * only the "outer" part of `classLikeType`:
 * the part with the outer classes and their
 * type arguments. Returns `null` otherwise.
 */
inline fun outerType(
    classLikeType: ConeClassLikeType,
    session: FirSession,
    outerClass: (FirClassLikeSymbol<*>) -> FirClassLikeSymbol<*>?,
): ConeClassLikeType? {
    val fullyExpandedType = classLikeType.fullyExpandedType(session)

    val symbol = fullyExpandedType.lookupTag.toSymbol(session) ?: return null

    if (symbol is FirRegularClassSymbol && !symbol.fir.isInner) return null

    val containingSymbol = outerClass(symbol) ?: return null
    val currentTypeArgumentsNumber = (symbol as? FirRegularClassSymbol)?.fir?.typeParameters?.count { it is FirTypeParameter } ?: 0

    return containingSymbol.constructType(
        fullyExpandedType.typeArguments.drop(currentTypeArgumentsNumber).toTypedArray(),
        isNullable = false
    )
}
