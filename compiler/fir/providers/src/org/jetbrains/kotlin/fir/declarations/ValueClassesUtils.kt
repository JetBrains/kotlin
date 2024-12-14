/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.descriptors.createValueClassRepresentation
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.createTypeSubstitutorByTypeConstructor
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun ConeKotlinType.substitutedUnderlyingTypeForInlineClass(session: FirSession, context: ConeTypeContext): ConeKotlinType? {
    val unsubstitutedType = unsubstitutedUnderlyingTypeForInlineClass(session) ?: return null
    val substitutor = createTypeSubstitutorByTypeConstructor(
        mapOf(this.typeConstructor(context) to this), context, approximateIntegerLiterals = true
    )
    return substitutor.substituteOrNull(unsubstitutedType)
}

internal fun ConeKotlinType.unsubstitutedUnderlyingTypeForInlineClass(session: FirSession): ConeKotlinType? {
    val symbol = this.fullyExpandedType(session).toRegularClassSymbol(session) ?: return null
    symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    return symbol.fir.inlineClassRepresentation?.underlyingType
}

fun computeValueClassRepresentation(klass: FirRegularClass, session: FirSession): ValueClassRepresentation<ConeRigidType>? {
    val parameters = klass.getValueClassUnderlyingParameters(session)?.takeIf { it.isNotEmpty() } ?: return null
    val fields = parameters.map { it.name to it.symbol.resolvedReturnType as ConeRigidType }
    fields.singleOrNull()?.let { (name, type) ->
        if (isRecursiveSingleFieldValueClass(type, session, mutableSetOf(type))) { // escape stack overflow
            return InlineClassRepresentation(name, type)
        }
    }

    return createValueClassRepresentation(session.typeContext, fields)
}

private fun FirRegularClass.getValueClassUnderlyingParameters(session: FirSession): List<FirValueParameter>? {
    if (!isInlineOrValue) return null
    return primaryConstructorIfAny(session)?.fir?.valueParameters
}

private fun isRecursiveSingleFieldValueClass(
    type: ConeRigidType,
    session: FirSession,
    visited: MutableSet<ConeRigidType>
): Boolean {
    val nextType = type.valueClassRepresentationTypeMarkersList(session)?.singleOrNull()?.second ?: return false
    return !visited.add(nextType) || isRecursiveSingleFieldValueClass(nextType, session, visited)
}

private fun ConeRigidType.valueClassRepresentationTypeMarkersList(session: FirSession): List<Pair<Name, ConeRigidType>>? {
    val symbol = this.toRegularClassSymbol(session) ?: return null
    if (!symbol.fir.isInlineOrValue) return null
    symbol.fir.valueClassRepresentation?.let { return it.underlyingPropertyNamesToTypes }

    val constructorSymbol = symbol.fir.primaryConstructorIfAny(session) ?: return null
    return constructorSymbol.valueParameterSymbols.map { it.name to it.resolvedReturnType as ConeRigidType }
}

fun FirSimpleFunction.isTypedEqualsInValueClass(session: FirSession): Boolean =
    containingClassLookupTag()?.toRegularClassSymbol(session)?.run {
        val valueClassStarProjection = this@run.defaultType().replaceArgumentsWithStarProjections()
        with(this@isTypedEqualsInValueClass) {
            contextParameters.isEmpty() && receiverParameter == null
                    && name == OperatorNameConventions.EQUALS
                    && this@run.isInlineOrValue && valueParameters.size == 1
                    && returnTypeRef.coneType.fullyExpandedType(session).let {
                it.isBoolean || it.isNothing
            } && valueParameters[0].returnTypeRef.coneType.let {
                it is ConeClassLikeType && it.replaceArgumentsWithStarProjections() == valueClassStarProjection
            }
        }
    } == true
