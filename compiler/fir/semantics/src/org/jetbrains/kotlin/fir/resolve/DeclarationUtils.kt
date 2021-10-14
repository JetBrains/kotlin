/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
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
            val containingDeclarationId = ClassId(classId.packageFqName, parentId, false)
            return session.symbolProvider.getClassLikeSymbolByClassId(containingDeclarationId)?.fir
        }
    }

    return null
}

fun isValidTypeParameterFromOuterClass(
    typeParameterSymbol: FirTypeParameterSymbol,
    classDeclaration: FirRegularClass?,
    session: FirSession
): Boolean {
    if (classDeclaration == null) {
        return true  // Extra check is required because of classDeclaration will be resolved later
    }

    fun containsTypeParameter(currentClassDeclaration: FirRegularClass): Boolean {
        if (currentClassDeclaration.typeParameters.any { it.symbol == typeParameterSymbol }) {
            return true
        }

        for (superTypeRef in currentClassDeclaration.superTypeRefs) {
            val superClassFir = superTypeRef.firClassLike(session)
            if (superClassFir == null || superClassFir is FirRegularClass && containsTypeParameter(superClassFir)) {
                return true
            }
        }

        return false
    }

    return containsTypeParameter(classDeclaration)
}

fun FirTypeRef.firClassLike(session: FirSession): FirClassLikeDeclaration? {
    val type = coneTypeSafe<ConeClassLikeType>() ?: return null
    return type.lookupTag.toSymbol(session)?.fir
}

fun List<FirQualifierPart>.toTypeProjections(): Array<ConeTypeProjection> =
    asReversed().flatMap { it.typeArgumentList.typeArguments.map { typeArgument -> typeArgument.toConeTypeProjection() } }.toTypedArray()

private object TypeAliasConstructorKey : FirDeclarationDataKey()

var FirConstructor.originalConstructorIfTypeAlias: FirConstructor? by FirDeclarationDataRegistry.data(TypeAliasConstructorKey)

val FirConstructorSymbol.isTypeAliasedConstructor: Boolean
    get() = fir.originalConstructorIfTypeAlias != null
