/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.FirTypeResolutionResult
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedDefaultValueInFunctionType
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildUserTypeRef

class FirSpecificTypeResolverTransformer(
    override val session: FirSession,
    private val errorTypeAsResolved: Boolean = true,
    private val resolveDeprecations: Boolean = true,
    private val supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default
) : FirAbstractTreeTransformer<ScopeClassDeclaration>(phase = FirResolvePhase.SUPER_TYPES) {
    private val typeResolver = session.typeResolver

    @set:PrivateForInline
    var areBareTypesAllowed: Boolean = false

    @OptIn(PrivateForInline::class)
    inline fun <R> withBareTypes(allowed: Boolean = true, block: () -> R): R {
        val oldValue = areBareTypesAllowed
        areBareTypesAllowed = allowed
        return try {
            block()
        } finally {
            areBareTypesAllowed = oldValue
        }
    }

    @set:PrivateForInline
    var isOperandOfIsOperator: Boolean = false

    @OptIn(PrivateForInline::class)
    inline fun <R> withIsOperandOfIsOperator(block: () -> R): R {
        val oldValue = isOperandOfIsOperator
        isOperandOfIsOperator = true
        return try {
            block()
        } finally {
            isOperandOfIsOperator = oldValue
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
    override fun transformTypeRef(typeRef: FirTypeRef, data: ScopeClassDeclaration): FirResolvedTypeRef {
        val scopeOwnerLookupNames = data.scopes.flatMap { it.scopeOwnerLookupNames }
        session.lookupTracker?.recordTypeLookup(typeRef, scopeOwnerLookupNames, currentFile?.source)
        withBareTypes(allowed = false) {
            typeRef.transformChildren(this, data)
        }
        val (resolvedType, diagnostic) = resolveType(typeRef, data)
        return transformType(typeRef, resolvedType, diagnostic, data)
    }

    @OptIn(PrivateForInline::class)
    override fun transformFunctionTypeRef(
        functionTypeRef: FirFunctionTypeRef,
        data: ScopeClassDeclaration
    ): FirResolvedTypeRef {
        functionTypeRef.transformChildren(this, data)
        val scopeOwnerLookupNames = data.scopes.flatMap { it.scopeOwnerLookupNames }
        session.lookupTracker?.recordTypeLookup(functionTypeRef, scopeOwnerLookupNames, currentFile?.source)
        val resolvedTypeWithDiagnostic = resolveType(functionTypeRef, data)
        val resolvedType = resolvedTypeWithDiagnostic.type.takeIfAcceptable()
        val diagnostic = resolvedTypeWithDiagnostic.diagnostic
        return if (resolvedType != null && resolvedType !is ConeErrorType && diagnostic == null) {
            buildResolvedTypeRef {
                source = functionTypeRef.source
                type = resolvedType
                annotations += functionTypeRef.annotations
                delegatedTypeRef = functionTypeRef
            }
        } else {
            buildErrorTypeRef {
                source = functionTypeRef.source
                if (resolvedType != null) {
                    type = resolvedType
                }
                this.diagnostic = diagnostic ?: (resolvedType as? ConeErrorType)?.diagnostic
                        ?: ConeSimpleDiagnostic("Unresolved function type: ${functionTypeRef.render()}")
            }
        }
    }

    @OptIn(PrivateForInline::class)
    private fun FirSpecificTypeResolverTransformer.resolveType(
        typeRef: FirTypeRef,
        scopeClassDeclaration: ScopeClassDeclaration,
    ): FirTypeResolutionResult {
        return typeResolver.resolveType(
            typeRef,
            scopeClassDeclaration,
            areBareTypesAllowed,
            isOperandOfIsOperator,
            resolveDeprecations,
            currentFile,
            supertypeSupplier
        )
    }

    private fun transformType(
        typeRef: FirTypeRef,
        resolvedType: ConeKotlinType,
        diagnostic: ConeDiagnostic?,
        scopeClassDeclaration: ScopeClassDeclaration,
    ): FirResolvedTypeRef {
        return when {
            resolvedType is ConeErrorType -> {
                buildErrorTypeRef {
                    val typeRefSourceKind = typeRef.source?.kind
                    val diagnosticSource = (resolvedType.diagnostic as? ConeUnexpectedTypeArgumentsError)?.source

                    source = if (diagnosticSource != null) {
                        if (typeRefSourceKind is KtFakeSourceElementKind) {
                            diagnosticSource.fakeElement(typeRefSourceKind)
                        } else {
                            diagnosticSource
                        }
                    } else {
                        typeRef.source
                    }

                    delegatedTypeRef = typeRef
                    type = resolvedType
                    partiallyResolvedTypeRef = tryCalculatingPartiallyResolvedTypeRef(typeRef, scopeClassDeclaration)

                    this.diagnostic = resolvedType.diagnostic
                }
            }
            diagnostic != null -> {
                buildErrorTypeRef {
                    source = typeRef.source
                    this.diagnostic = diagnostic
                    type = resolvedType
                    delegatedTypeRef = typeRef
                    partiallyResolvedTypeRef = tryCalculatingPartiallyResolvedTypeRef(typeRef, scopeClassDeclaration)
                }
            }
            else -> {
                buildResolvedTypeRef {
                    source = typeRef.source
                    type = resolvedType
                    annotations += typeRef.annotations
                    delegatedTypeRef = typeRef
                }
            }
        }
    }

    /**
     * Tries to calculate a partially resolved type reference for a type reference which was resolved to an error type.
     * It will attempt to resolve the type with a decreasing number of qualifiers until it succeeds, allowing
     * partial resolution in case of errors in the type reference.
     *
     * This is useful for providing better IDE support when resolving partially incorrect types.
     *
     * @param typeRef The type reference for which to try to calculate a partially resolved type reference.
     * @param data The scope class declaration containing relevant information for resolving the reference.
     * @return A partially resolved type reference if it was resolved, or `null` otherwise.
     */
    private fun tryCalculatingPartiallyResolvedTypeRef(typeRef: FirTypeRef, data: ScopeClassDeclaration): FirTypeRef? {
        if (typeRef !is FirUserTypeRef) return null
        val qualifiers = typeRef.qualifier
        if (qualifiers.size <= 1) {
            return null
        }
        val qualifiersToTry = qualifiers.toMutableList()
        while (qualifiersToTry.size > 1) {
            qualifiersToTry.removeLast()
            val typeRefToTry = buildUserTypeRef {
                qualifier += qualifiersToTry
                isMarkedNullable = false
            }
            val (resolvedType, diagnostic) = resolveType(typeRefToTry, data)
            if (resolvedType is ConeErrorType || diagnostic != null) continue
            return buildResolvedTypeRef {
                source = qualifiersToTry.last().source
                type = resolvedType
                delegatedTypeRef = typeRefToTry
            }
        }
        return null
    }

    private fun ConeKotlinType.takeIfAcceptable(): ConeKotlinType? = this.takeUnless {
        !errorTypeAsResolved && it is ConeErrorType
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: ScopeClassDeclaration): FirTypeRef {
        return resolvedTypeRef
    }

    override fun transformErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: ScopeClassDeclaration): FirTypeRef {
        errorTypeRef.transformPartiallyResolvedTypeRef(this, data)
        return errorTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: ScopeClassDeclaration): FirTypeRef {
        return implicitTypeRef
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ScopeClassDeclaration): FirStatement {
        val result = transformElement(valueParameter, data)
        result.defaultValue?.let {
            it.resultType = buildErrorTypeRef {
                diagnostic = ConeUnsupportedDefaultValueInFunctionType(it.source)
            }
        }
        return result
    }
}
