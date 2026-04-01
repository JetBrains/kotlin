/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.SuspiciousValueClassCheck
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun ConeKotlinType.unsubstitutedUnderlyingTypeForInlineClass(session: FirSession): ConeKotlinType? {
    val symbol = this.fullyExpandedType(session).toRegularClassSymbol(session) ?: return null
    symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    return symbol.fir.inlineClassRepresentation?.underlyingType
}

@OptIn(SuspiciousValueClassCheck::class)
fun computeValueClassRepresentation(klass: FirRegularClass, session: FirSession): ValueClassRepresentation<ConeRigidType>? {
    val areExtendedValueClassesSupported = session.languageVersionSettings.supportsFeature(LanguageFeature.ValueClasses)
    if (areExtendedValueClassesSupported && !klass.hasAnnotation(JVM_INLINE_ANNOTATION_CLASS_ID, session) && klass.isValue) {
        return ExtendedValueClassRepresentation()
    }
    val parameters = klass.getValueClassUnderlyingParameters(session)?.takeIf { it.isNotEmpty() } ?: return null
    val fields = parameters.map { it.name to it.symbol.resolvedReturnType as ConeRigidType }
    fields.singleOrNull()?.let { [name, type] ->
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
    symbol.fir.valueClassRepresentation?.let { return it.asBasic()?.underlyingPropertyNamesToTypes }

    val constructorSymbol = symbol.fir.primaryConstructorIfAny(session) ?: return null
    return constructorSymbol.valueParameterSymbols.map { it.name to it.resolvedReturnType as ConeRigidType }
}

fun FirNamedFunctionSymbol.isTypedEqualsInValueClass(session: FirSession): Boolean =
    containingClassLookupTag()?.toRegularClassSymbol(session)?.run {
        val valueClassStarProjection = this@run.defaultType().replaceArgumentsWithStarProjections()
        with(this@isTypedEqualsInValueClass) {
            contextParameterSymbols.isEmpty() && receiverParameterSymbol == null
                    && name == OperatorNameConventions.EQUALS
                    && this@run.isInlineOrValue && valueParameterSymbols.size == 1
                    && resolvedReturnTypeRef.coneType.fullyExpandedType(session).let {
                it.isBoolean || it.isNothing
            } && valueParameterSymbols[0].resolvedReturnTypeRef.coneType.let {
                it is ConeClassLikeType && it.replaceArgumentsWithStarProjections() == valueClassStarProjection
            }
        }
    } == true
