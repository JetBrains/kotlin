/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.SuspiciousValueClassCheck
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.util.OperatorNameConventions

@OptIn(ValueClassBackendAgnosticApi::class)
internal fun ConeKotlinType.unsubstitutedUnderlyingTypeForInlineClassInJvm(session: FirSession): ConeKotlinType? {
    val symbol = this.fullyExpandedType(session).toRegularClassSymbol(session) ?: return null
    // All the usages are JVM-only, so treatFullValueClassesWithOneFieldAsBasic is effectively false
    return symbol.inlineClassRepresentation(treatCompatibleFullValueClassesAsInline = false)?.underlyingType
}

@OptIn(SuspiciousValueClassCheck::class)
fun computeValueClassRepresentation(klass: FirRegularClass, session: FirSession): ValueClassRepresentation<ConeRigidType>? {
    val areFullValueClassesSupported = session.languageVersionSettings.supportsFeature(LanguageFeature.FullValueClasses)
    val jvmInlineAnnotationClassId = session.annotationPlatformSupport.jvmInlineAnnotationClassId
    if (areFullValueClassesSupported && (jvmInlineAnnotationClassId == null || !klass.hasAnnotation(jvmInlineAnnotationClassId, session)) && klass.isValue) {
        val fields = if (klass.modality == Modality.ABSTRACT || klass.modality == Modality.SEALED) {
            null
        } else {
            klass.getValueClassUnderlyingParameters(session)
                ?.map { it.name to it.symbol.resolvedReturnType as ConeRigidType }
                ?: emptyList()
        }
        return FullValueClassRepresentation(fields)
    }
    return klass.getValueClassUnderlyingParameters(session)
        ?.takeIf { it.isNotEmpty() }
        ?.singleOrNull()
        ?.let { InlineClassRepresentation(it.name, it.symbol.resolvedReturnType as ConeRigidType) }
}

private fun FirRegularClass.getValueClassUnderlyingParameters(session: FirSession): List<FirValueParameter>? {
    if (!isInlineOrValue) return null
    return primaryConstructorIfAny(session)?.fir?.valueParameters
}

fun FirNamedFunctionSymbol.isTypedEqualsInValueClass(session: FirSession): Boolean =
    containingClassLookupTag()?.toRegularClassSymbol(session)?.run {
        val valueClassStarProjection = this@run.defaultType().replaceArgumentsWithStarProjections()
        with(this@isTypedEqualsInValueClass) {
            contextParameterSymbols.isEmpty() && receiverParameterSymbol == null
                    && name == OperatorNameConventions.EQUALS
                    && this@run.isBasicValueClass && valueParameterSymbols.size == 1
                    && resolvedReturnTypeRef.coneType.fullyExpandedType(session).let {
                it.isBoolean || it.isNothing
            } && valueParameterSymbols[0].resolvedReturnTypeRef.coneType.let {
                it is ConeClassLikeType && it.replaceArgumentsWithStarProjections() == valueClassStarProjection
            }
        }
    } == true
