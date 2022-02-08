/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.getAnnotationsByClassId
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.WeakPair

fun ConeClassifierLookupTag.toSymbol(useSiteSession: FirSession): FirClassifierSymbol<*>? =
    when (this) {
        is ConeClassLikeLookupTag -> toSymbol(useSiteSession)
        is ConeClassifierLookupTagWithFixedSymbol -> this.symbol
        else -> null
    }

@OptIn(LookupTagInternals::class)
fun ConeClassLikeLookupTag.toSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
    if (this is ConeClassLookupTagWithFixedSymbol) {
        return this.symbol
    }
    val firSymbolProvider = useSiteSession.symbolProvider
    (this as? ConeClassLikeLookupTagImpl)?.boundSymbol?.takeIf { it.first === useSiteSession }?.let { return it.second }

    return firSymbolProvider.getClassLikeSymbolByClassId(classId).also {
        (this as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(useSiteSession, it)
    }
}

@OptIn(LookupTagInternals::class)
fun ConeClassLikeLookupTag.toSymbolOrError(useSiteSession: FirSession): FirClassLikeSymbol<*> =
    toSymbol(useSiteSession)
        ?: error("Class symbol with classId $classId was not found")

@OptIn(LookupTagInternals::class)
fun ConeClassLikeLookupTag.toFirRegularClassSymbol(session: FirSession): FirRegularClassSymbol? =
    session.symbolProvider.getSymbolByLookupTag(this) as? FirRegularClassSymbol

@OptIn(LookupTagInternals::class)
fun ConeClassLikeLookupTagImpl.bindSymbolToLookupTag(session: FirSession, symbol: FirClassLikeSymbol<*>?) {
    boundSymbol = WeakPair(session, symbol)
}

@LookupTagInternals
fun ConeClassLikeLookupTag.toFirRegularClass(session: FirSession): FirRegularClass? =
    session.symbolProvider.getSymbolByLookupTag(this)?.fir as? FirRegularClass

fun FirSymbolProvider.getSymbolByLookupTag(lookupTag: ConeClassifierLookupTag): FirClassifierSymbol<*>? {
    return lookupTag.toSymbol(session)
}

fun FirSymbolProvider.getSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): FirClassLikeSymbol<*>? {
    return lookupTag.toSymbol(session)
}

fun ConeKotlinType.withParameterNameAnnotation(valueParameter: FirValueParameter): ConeKotlinType {
    if (valueParameter.name == SpecialNames.NO_NAME_PROVIDED || valueParameter.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) return this
    // Existing @ParameterName annotation takes precedence
    if (attributes.customAnnotations.getAnnotationsByClassId(StandardNames.FqNames.parameterNameClassId).isNotEmpty()) return this

    val fakeSource = valueParameter.source?.fakeElement(KtFakeSourceElementKind.ParameterNameAnnotationCall)
    val parameterNameAnnotationCall = buildAnnotation {
        source = fakeSource
        annotationTypeRef =
            buildResolvedTypeRef {
                source = fakeSource
                type = ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(StandardNames.FqNames.parameterNameClassId),
                    emptyArray(),
                    isNullable = false
                )
            }
        argumentMapping = buildAnnotationArgumentMapping {
            mapping[StandardClassIds.Annotations.ParameterNames.parameterNameName] =
                buildConstExpression(fakeSource, ConstantValueKind.String, valueParameter.name.asString(), setType = true)
        }
    }
    val attributesWithParameterNameAnnotation =
        ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(listOf(parameterNameAnnotationCall))))
    return withCombinedAttributesFrom(attributesWithParameterNameAnnotation)
}

fun ConeKotlinType.withCombinedAttributesFrom(other: ConeKotlinType): ConeKotlinType =
    withCombinedAttributesFrom(other.attributes)

private fun ConeKotlinType.withCombinedAttributesFrom(other: ConeAttributes): ConeKotlinType {
    if (other.isEmpty()) return this
    val combinedConeAttributes = attributes.add(other)
    return withAttributes(combinedConeAttributes)
}

fun ConeKotlinType.findClassRepresentation(
    dispatchReceiverParameterType: ConeKotlinType,
    session: FirSession
): ConeClassLikeLookupTag? =
    when (this) {
        is ConeClassLikeType -> this.fullyExpandedType(session).lookupTag
        is ConeFlexibleType -> lowerBound.findClassRepresentation(dispatchReceiverParameterType, session)
        is ConeCapturedType -> constructor.supertypes.orEmpty()
            .findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeDefinitelyNotNullType -> original.findClassRepresentation(dispatchReceiverParameterType, session)
        is ConeIntegerLiteralType -> possibleTypes.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeIntersectionType -> intersectedTypes.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeTypeParameterType -> lookupTag.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeTypeVariableType -> (this.lookupTag.originalTypeParameter as? ConeTypeParameterLookupTag)
            ?.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeStubType -> (this.constructor.variable.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)
            ?.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeLookupTagBasedType -> null
    }

private fun ConeTypeParameterLookupTag.findClassRepresentationThatIsSubtypeOf(
    supertype: ConeKotlinType,
    session: FirSession
): ConeClassLikeLookupTag? =
    typeParameterSymbol.resolvedBounds.map { it.coneType }.findClassRepresentationThatIsSubtypeOf(supertype, session)

private fun Collection<ConeKotlinType>.findClassRepresentationThatIsSubtypeOf(
    supertype: ConeKotlinType,
    session: FirSession
): ConeClassLikeLookupTag? = firstOrNull { it.isSubtypeOf(supertype, session) }?.findClassRepresentation(supertype, session)

