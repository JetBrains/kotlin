/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirSpecificTypeResolverTransformer(
    override val session: FirSession,
    private val errorTypeAsResolved: Boolean = true
) : FirAbstractTreeTransformer<FirScope>(phase = FirResolvePhase.SUPER_TYPES) {
    private val typeResolver = session.typeResolver

    @set:PrivateForInline
    var areBareTypesAllowed: Boolean = false

    @OptIn(PrivateForInline::class)
    inline fun <R> withAllowedBareTypes(block: () -> R): R {
        val oldValue = areBareTypesAllowed
        areBareTypesAllowed = true
        return try {
            block()
        } finally {
            areBareTypesAllowed = oldValue
        }
    }

    @PrivateForInline
    @JvmField
    var currentFile: FirFile? = null

    @OptIn(PrivateForInline::class)
    inline fun <R> withFile(file: FirFile?, block: FirSpecificTypeResolverTransformer.() -> R): R {
        val oldValue = currentFile
        currentFile = file
        return try {
            block()
        } finally {
            currentFile = oldValue
        }
    }

    @OptIn(PrivateForInline::class)
    override fun transformTypeRef(typeRef: FirTypeRef, data: FirScope): CompositeTransformResult<FirResolvedTypeRef> {
        session.lookupTracker?.recordTypeLookup(typeRef, data.scopeOwnerLookupNames, currentFile?.source)
        typeRef.transformChildren(this, data)
        return transformType(typeRef, typeResolver.resolveType(typeRef, data, areBareTypesAllowed))
    }

    @OptIn(PrivateForInline::class)
    override fun transformFunctionTypeRef(
        functionTypeRef: FirFunctionTypeRef,
        data: FirScope
    ): CompositeTransformResult<FirResolvedTypeRef> {
        functionTypeRef.transformChildren(this, data)
        session.lookupTracker?.recordTypeLookup(functionTypeRef, data.scopeOwnerLookupNames, currentFile?.source)
        val resolvedType = typeResolver.resolveType(functionTypeRef, data, areBareTypesAllowed).takeIfAcceptable()
        return if (resolvedType != null && resolvedType !is ConeClassErrorType) {
            buildResolvedTypeRef {
                source = functionTypeRef.source
                type = resolvedType
                annotations += functionTypeRef.annotations
                delegatedTypeRef = functionTypeRef
            }
        } else {
            buildErrorTypeRef {
                source = functionTypeRef.source
                diagnostic = (resolvedType as? ConeClassErrorType)?.diagnostic
                    ?: ConeSimpleDiagnostic("Unresolved functional type: ${functionTypeRef.render()}")
            }
        }.compose()
    }

    private fun transformType(typeRef: FirTypeRef, resolvedType: ConeKotlinType): CompositeTransformResult<FirResolvedTypeRef> {
        return if (resolvedType !is ConeClassErrorType) {
            buildResolvedTypeRef {
                source = typeRef.source
                type = resolvedType
                annotations += typeRef.annotations
                delegatedTypeRef = typeRef
            }
        } else {
            buildErrorTypeRef {
                val typeRefSourceKind = typeRef.source?.kind
                val diagnosticSource = resolvedType.diagnostic.safeAs<ConeUnexpectedTypeArgumentsError>()
                    ?.source.safeAs<FirSourceElement>()

                source = if (diagnosticSource != null) {
                    if (typeRefSourceKind is FirFakeSourceElementKind) {
                        diagnosticSource.fakeElement(typeRefSourceKind)
                    } else {
                        diagnosticSource
                    }
                } else {
                    typeRef.source
                }

                delegatedTypeRef = typeRef

                diagnostic = resolvedType.diagnostic
            }
        }.compose()
    }

    private fun ConeKotlinType.takeIfAcceptable(): ConeKotlinType? = this.takeUnless {
        !errorTypeAsResolved && it is ConeClassErrorType
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: FirScope): CompositeTransformResult<FirTypeRef> {
        return resolvedTypeRef.compose()
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: FirScope): CompositeTransformResult<FirTypeRef> {
        return implicitTypeRef.compose()
    }
}
