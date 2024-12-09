/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.SuspiciousValueClassCheck
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.java.JvmSupertypeUpdater.DelegatedConstructorCallTransformer.Companion.recordType
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.PlatformSupertypeUpdater
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds

class JvmSupertypeUpdater(private val session: FirSession) : PlatformSupertypeUpdater() {
    private val jvmRecordUpdater = DelegatedConstructorCallTransformer(session)

    @OptIn(SuspiciousValueClassCheck::class)
    override fun updateSupertypesIfNeeded(firClass: FirClass, scopeSession: ScopeSession) {
        when {
            firClass !is FirRegularClass -> return
            firClass.isData && firClass.hasAnnotationSafe(JvmStandardClassIds.Annotations.JvmRecord, session) -> {}
            firClass.classKind == ClassKind.CLASS &&
                    firClass.isValue &&
                    !firClass.hasAnnotationSafe(JvmStandardClassIds.Annotations.JvmInline, session) &&
                    session.jvmTargetProvider?.jvmTarget.let { it != null && it.majorVersion >= 23 } &&
                    session.languageVersionSettings.getFlag(JvmAnalysisFlags.enableJvmPreview) &&
                    session.languageVersionSettings.supportsFeature(LanguageFeature.ValhallaValueClasses) -> {}
            else -> return
        }
        var anyFound = false
        var hasExplicitSuperClass = false
        val newSuperTypeRefs = firClass.superTypeRefs.mapTo(mutableListOf()) {
            when {
                it is FirImplicitBuiltinTypeRef && it.id == StandardClassIds.Any -> {
                    anyFound = true
                    it.withReplacedConeType(recordType)
                }
                it.coneType.toRegularClassSymbol(session)?.classKind == ClassKind.CLASS -> {
                    hasExplicitSuperClass = true
                    it
                }
                else -> it
            }
        }
        if (!anyFound && !hasExplicitSuperClass) {
            newSuperTypeRefs += recordType.toFirResolvedTypeRef()
        }

        if (anyFound || !hasExplicitSuperClass) {
            firClass.replaceSuperTypeRefs(newSuperTypeRefs)
            firClass.transformDeclarations(jvmRecordUpdater, scopeSession)
        }
    }

    private class DelegatedConstructorCallTransformer(private val session: FirSession) : FirTransformer<ScopeSession>() {
        companion object {
            val recordType = JvmStandardClassIds.Java.Record.constructClassLikeType(emptyArray(), isMarkedNullable = false)
        }

        override fun <E : FirElement> transformElement(element: E, data: ScopeSession): E {
            return element
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: ScopeSession): FirStatement {
            return regularClass
        }

        override fun transformConstructor(constructor: FirConstructor, data: ScopeSession): FirStatement {
            return constructor.transformDelegatedConstructor(this, data)
        }

        override fun transformErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: ScopeSession) =
            transformConstructor(errorPrimaryConstructor, data)

        override fun transformDelegatedConstructorCall(
            delegatedConstructorCall: FirDelegatedConstructorCall,
            data: ScopeSession
        ): FirStatement {
            /*
             * Here we need to update only implicit calls to Any()
             * And such calls don't have a real source and can not be an lazy calls
             */
            if (
                delegatedConstructorCall is FirLazyDelegatedConstructorCall ||
                delegatedConstructorCall.source?.kind != KtFakeSourceElementKind.DelegatingConstructorCall
            ) return delegatedConstructorCall
            val constructedTypeRef = delegatedConstructorCall.constructedTypeRef
            if (constructedTypeRef is FirImplicitTypeRef || constructedTypeRef.coneTypeSafe<ConeKotlinType>()?.isAny == true) {
                delegatedConstructorCall.replaceConstructedTypeRef(constructedTypeRef.resolvedTypeFromPrototype(recordType))
            }

            val recordConstructorSymbol = recordType.lookupTag.toRegularClassSymbol(session)
                ?.unsubstitutedScope(session, data, withForcedTypeCalculator = false, memberRequiredPhase = null)
                ?.getDeclaredConstructors()
                ?.firstOrNull { it.fir.valueParameters.isEmpty() }

            if (recordConstructorSymbol != null) {
                val newReference = buildResolvedNamedReference {
                    name = JvmStandardClassIds.Java.Record.shortClassName
                    resolvedSymbol = recordConstructorSymbol
                }
                delegatedConstructorCall.replaceCalleeReference(newReference)
            }
            return delegatedConstructorCall
        }
    }

}
