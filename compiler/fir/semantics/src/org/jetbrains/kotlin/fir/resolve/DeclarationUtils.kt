/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JsLibrary
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JsNative
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JsNativeGetter
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JsNativeInvoke
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.JsNativeSetter
import org.jetbrains.kotlin.util.OperatorNameConventions

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

private object TypeAliasConstructorKey : FirDeclarationDataKey()

var FirConstructor.originalConstructorIfTypeAlias: FirConstructor? by FirDeclarationDataRegistry.data(TypeAliasConstructorKey)

val FirConstructorSymbol.isTypeAliasedConstructor: Boolean
    get() = fir.originalConstructorIfTypeAlias != null

fun FirSimpleFunction.isEquals(): Boolean {
    if (name != OperatorNameConventions.EQUALS) return false
    if (valueParameters.size != 1) return false
    val parameter = valueParameters.first()
    return parameter.returnTypeRef.isNullableAny
}

fun FirDeclaration.isEffectivelyExternal(session: FirSession): Boolean {
    if (this is FirMemberDeclaration && isExternal) return true

    if (this is FirPropertyAccessor) {
        val property = propertySymbol?.fir ?: error("Should've had a property")
        if (property.isEffectivelyExternal(session)) return true
    }

    if (this is FirProperty) {
        if (getter?.isExternal == true && (!isVar || setter?.isExternal == true)) {
            return true
        }
    }

    return getContainingClassSymbol(session)?.fir?.isEffectivelyExternal(session) == true
}

fun FirDeclaration.isNativeObject(session: FirSession): Boolean {
    if (hasAnnotationOrInsideAnnotatedClass(JsNative, session) || isEffectivelyExternal(session)) {
        return true
    }

    if (this is FirPropertyAccessor) {
        val property = propertySymbol?.fir ?: error("Should've had a property")
        return property.hasAnnotationOrInsideAnnotatedClass(JsNative, session)
    }

    return if (this is FirAnonymousInitializer) {
        getContainingClassSymbol(session)?.fir?.isNativeObject(session) == true
    } else {
        false
    }
}

fun FirDeclaration.isEffectivelyExternalMember(session: FirSession): Boolean {
    return this is FirMemberDeclaration && isEffectivelyExternal(session)
}

val PREDEFINED_ANNOTATIONS = setOf(JsLibrary, JsNative, JsNativeInvoke, JsNativeGetter, JsNativeSetter)

fun FirDeclaration.isPredefinedObject(session: FirSession): Boolean {
    if (this is FirMemberDeclaration && (isExpect)) return true
    if (isEffectivelyExternalMember(session)) return true

    for (annotation in PREDEFINED_ANNOTATIONS) {
        if (hasAnnotationOrInsideAnnotatedClass(annotation, session)) {
            return true
        }
    }

    return false
}
