/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.java.JvmSupertypeUpdater.DelegatedConstructorCallTransformer.Companion.recordType
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.PlatformSupertypeUpdater
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.StandardClassIds

class JvmSupertypeUpdater(private val session: FirSession) : PlatformSupertypeUpdater() {
    private val jvmRecordUpdater = DelegatedConstructorCallTransformer(session)

    override fun updateSupertypesIfNeeded(firClass: FirClass, scopeSession: ScopeSession) {
        if (!(firClass is FirRegularClass && firClass.isData && firClass.hasAnnotationSafe(StandardClassIds.Annotations.JvmRecord))) return
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
            firClass.transformSingle(jvmRecordUpdater, scopeSession)
        }
    }

    private class DelegatedConstructorCallTransformer(private val session: FirSession) : FirTransformer<ScopeSession>() {
        companion object {
            val recordType = StandardClassIds.Java.Record.constructClassLikeType(emptyArray(), isNullable = false)
        }

        override fun <E : FirElement> transformElement(element: E, data: ScopeSession): E {
            return element
        }

        override fun transformRegularClass(regularClass: FirRegularClass, data: ScopeSession): FirStatement {
            return regularClass.transformDeclarations(this, data)
        }

        override fun transformConstructor(constructor: FirConstructor, data: ScopeSession): FirStatement {
            return constructor.transformDelegatedConstructor(this, data)
        }

        override fun transformDelegatedConstructorCall(
            delegatedConstructorCall: FirDelegatedConstructorCall,
            data: ScopeSession
        ): FirStatement {
            val constructedTypeRef = delegatedConstructorCall.constructedTypeRef
            if (constructedTypeRef is FirImplicitTypeRef || constructedTypeRef.coneTypeSafe<ConeKotlinType>()?.isAny == true) {
                delegatedConstructorCall.replaceConstructedTypeRef(constructedTypeRef.resolvedTypeFromPrototype(recordType))
            }

            val recordConstructorSymbol = recordType.lookupTag.toFirRegularClassSymbol(session)
                ?.unsubstitutedScope(session, data, withForcedTypeCalculator = false)
                ?.getDeclaredConstructors()
                ?.firstOrNull { it.fir.valueParameters.isEmpty() }

            if (recordConstructorSymbol != null) {
                val newReference = buildResolvedNamedReference {
                    name = StandardClassIds.Java.Record.shortClassName
                    resolvedSymbol = recordConstructorSymbol
                }
                delegatedConstructorCall.replaceCalleeReference(newReference)
            }
            return delegatedConstructorCall
        }
    }

}
