/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.WeakPair

fun FirClassLikeSymbol<*>.getClassAndItsOuterClassesWhenLocal(session: FirSession): Set<FirClassLikeSymbol<*>> =
    generateSequence(this.takeIf { it.isLocal }) {
        if (it.isInner) it.getContainingClassLookupTag()?.toRegularClassSymbol(session) else null
    }.toSet()

@LookupTagInternals
fun ConeClassLikeLookupTagImpl.bindSymbolToLookupTag(session: FirSession, symbol: FirClassLikeSymbol<*>?) {
    boundSymbol = WeakPair(session, symbol)
}

fun ConeKotlinType.withParameterNameAnnotation(parameter: FirFunctionTypeParameter): ConeKotlinType {
    val name = parameter.name
    if (name == null || name == SpecialNames.NO_NAME_PROVIDED || name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) return this
    // Existing @ParameterName annotation takes precedence
    if (attributes.parameterNameAttribute != null) return this

    val fakeSource = parameter.source?.fakeElement(KtFakeSourceElementKind.ParameterNameAnnotationCall)
    val parameterNameAnnotationCall = buildAnnotation {
        source = fakeSource
        annotationTypeRef =
            buildResolvedTypeRef {
                source = fakeSource
                coneType = ConeClassLikeTypeImpl(
                    StandardNames.FqNames.parameterNameClassId.toLookupTag(),
                    emptyArray(),
                    isMarkedNullable = false
                )
            }
        argumentMapping = buildAnnotationArgumentMapping {
            mapping[StandardClassIds.Annotations.ParameterNames.parameterNameName] =
                buildLiteralExpression(fakeSource, ConstantValueKind.String, name.asString(), setType = true)
        }
    }
    return withAttributes(attributes.add(ParameterNameTypeAttribute(parameterNameAnnotationCall)))
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
        is ConeDynamicType -> upperBound.findClassRepresentation(dispatchReceiverParameterType, session)
        is ConeFlexibleType -> lowerBound.findClassRepresentation(dispatchReceiverParameterType, session)
        is ConeCapturedType -> constructor.supertypes.orEmpty()
            .findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeDefinitelyNotNullType -> original.findClassRepresentation(dispatchReceiverParameterType, session)
        is ConeIntegerLiteralType -> possibleTypes.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeIntersectionType -> intersectedTypes.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeTypeParameterType -> lookupTag.findClassRepresentationThatIsSubtypeOf(dispatchReceiverParameterType, session)
        is ConeTypeVariableType -> (this.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)
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
): ConeClassLikeLookupTag? {
    val supertypeLowerBound = supertype.lowerBoundIfFlexible()
    val compatibleComponent = this.firstOrNull {
        it.isSubtypeOf(supertypeLowerBound, session)
    } ?: return null
    return compatibleComponent.findClassRepresentation(supertypeLowerBound, session)
}
