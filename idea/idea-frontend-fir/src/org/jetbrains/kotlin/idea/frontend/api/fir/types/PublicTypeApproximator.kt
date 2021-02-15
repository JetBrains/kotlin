/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.Variance

internal object PublicTypeApproximator {
    fun approximateTypeToPublicDenotable(
        type: ConeKotlinType,
        context: ConeInferenceContext,
        session: FirSession,
    ): ConeKotlinType? = approximate(type, context, session) as ConeKotlinType?

    private fun approximate(
        type: ConeKotlinType,
        context: ConeInferenceContext,
        session: FirSession,
    ): ConeTypeProjection? = when (type) {
        is ConeFlexibleType -> approximate(type.upperBound, context, session)
        is ConeCapturedType -> type
        is ConeDefinitelyNotNullType -> ConeDefinitelyNotNullType(
            approximate(
                type.original,
                context,
                session,
            ) as ConeKotlinType
        )
        is ConeIntegerLiteralType -> type
        is ConeIntersectionType -> context.commonSuperTypeOrNull(type.intersectedTypes.toList())?.let { approximate(it, context, session) }
        is ConeLookupTagBasedType -> when (type) {
            is ConeTypeParameterType -> type
            is ConeClassErrorType -> null
            is ConeClassLikeTypeImpl -> approximateClassLikeType(type, context, session)
            else -> error("Unexpected type ${type::class}")
        }
        is ConeStubType -> null
    }

    private fun approximateClassLikeType(
        typeImpl: ConeClassLikeTypeImpl,
        context: ConeInferenceContext,
        session: FirSession,
    ): ConeTypeProjection? {
        val regularClass = typeImpl.classOrAnonymousClass(session) ?: return null
        val type = if (needApproximationAsSuperType(regularClass)) {
            firstSuperType(regularClass, session) ?: return null
        } else {
            typeImpl
        }

        val typeClassifier = type.classOrAnonymousClass(session) ?: return null

        val typeArguments = approximateTypeParameters(type, typeClassifier, context, session)?.toTypedArray() ?: return null
        return ConeClassLikeTypeImpl(
            type.lookupTag,
            typeArguments,
            type.isNullable,
            type.attributes
        )
    }

    private fun approximateTypeParameters(
        type: ConeClassLikeTypeImpl,
        typeClassifier: FirClass<*>,
        context: ConeInferenceContext,
        session: FirSession
    ): List<ConeTypeProjection>? {
        val result = mutableListOf<ConeTypeProjection>()
        if (type.typeArguments.size != typeClassifier.typeParameters.size) return null
        for ((typeArg, typeParam) in type.typeArguments.zip(typeClassifier.typeParameters)) {
            val variance = (typeParam as? FirTypeParameter)?.variance ?: return null
            result += approximateTypeProjection(typeArg, context, session, variance) ?: return null
        }
        return result
    }

    private fun firstSuperType(regularClass: FirClass<*>, session: FirSession): ConeClassLikeTypeImpl? {
        val superTypes = lookupSuperTypes(regularClass, lookupInterfaces = true, deep = false, session, substituteTypes = true)
        return superTypes.first() as? ConeClassLikeTypeImpl
    }

    fun ConeClassLikeTypeImpl.classOrAnonymousClass(session: FirSession): FirClass<*>? {
        val fir = lookupTag.toSymbol(session)?.fir ?: return null
        if (fir is FirAnonymousObject) return fir
        else return fir.fullyExpandedClass(session)
    }

    private fun needApproximationAsSuperType(fir: FirClass<*>) = when (fir) {
        is FirRegularClass -> fir.isLocal
        is FirAnonymousObject -> true
        else -> false
    }

    private fun approximateTypeProjection(
        typeProjection: ConeTypeProjection,
        context: ConeInferenceContext,
        session: FirSession,
        variance: Variance,
    ): ConeTypeProjection? {
        val type = when (typeProjection) {
            ConeStarProjection -> return ConeStarProjection
            is ConeKotlinTypeProjection -> typeProjection.type
            else -> error("Unexpected type ${typeProjection::class}")
        }
        val newType = when (val new = approximate(type, context, session)) {
            ConeStarProjection -> return ConeStarProjection
            is ConeKotlinTypeProjection -> new.type
            null -> return null
            else -> error("Unexpected type ${new::class}")
        }
        return when (variance) {
            Variance.INVARIANT -> when {
                with(context) { newType.typeConstructor().isAnyConstructor() } -> ConeStarProjection
                AbstractTypeChecker.equalTypes(context, type, newType) -> newType
                else -> ConeStarProjection
            }
            Variance.IN_VARIANCE -> if (AbstractTypeChecker.isSubtypeOf(context, newType, type)) newType else ConeStarProjection
            Variance.OUT_VARIANCE -> if (AbstractTypeChecker.isSubtypeOf(context, type, newType)) newType else ConeStarProjection
        }
    }
}