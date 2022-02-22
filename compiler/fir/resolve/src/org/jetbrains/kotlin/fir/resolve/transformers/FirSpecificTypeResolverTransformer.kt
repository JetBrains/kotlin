/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.SupertypeSupplier
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirSpecificTypeResolverTransformer(
    override val session: FirSession,
    private val errorTypeAsResolved: Boolean = true,
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
        val (resolvedType, diagnostic) = typeResolver.resolveType(
            typeRef,
            data,
            areBareTypesAllowed,
            isOperandOfIsOperator,
            currentFile,
            supertypeSupplier
        )
        return transformType(typeRef, resolvedType, diagnostic)
    }

    @OptIn(PrivateForInline::class)
    override fun transformFunctionTypeRef(
        functionTypeRef: FirFunctionTypeRef,
        data: ScopeClassDeclaration
    ): FirResolvedTypeRef {
        functionTypeRef.transformChildren(this, data)
        val scopeOwnerLookupNames = data.scopes.flatMap { it.scopeOwnerLookupNames }
        session.lookupTracker?.recordTypeLookup(functionTypeRef, scopeOwnerLookupNames, currentFile?.source)
        val resolvedTypeWithDiagnostic = typeResolver.resolveType(
            functionTypeRef,
            data,
            areBareTypesAllowed,
            isOperandOfIsOperator,
            currentFile,
            supertypeSupplier
        )
        val resolvedType = resolvedTypeWithDiagnostic.first.takeIfAcceptable()
        val diagnostic = resolvedTypeWithDiagnostic.second
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
                        ?: ConeSimpleDiagnostic("Unresolved functional type: ${functionTypeRef.render()}")
            }
        }
    }

    private fun transformType(typeRef: FirTypeRef, resolvedType: ConeKotlinType, diagnostic: ConeDiagnostic?): FirResolvedTypeRef {
        return when {
            resolvedType is ConeErrorType -> {
                buildErrorTypeRef {
                    val typeRefSourceKind = typeRef.source?.kind
                    val diagnosticSource = resolvedType.diagnostic.safeAs<ConeUnexpectedTypeArgumentsError>()
                        ?.source.safeAs<KtSourceElement>()

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

                    this.diagnostic = resolvedType.diagnostic
                }
            }
            diagnostic != null -> {
                buildErrorTypeRef {
                    source = typeRef.source
                    this.diagnostic = diagnostic
                    type = resolvedType
                    delegatedTypeRef = typeRef
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

    private fun ConeKotlinType.takeIfAcceptable(): ConeKotlinType? = this.takeUnless {
        !errorTypeAsResolved && it is ConeErrorType
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: ScopeClassDeclaration): FirTypeRef {
        return resolvedTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: ScopeClassDeclaration): FirTypeRef {
        return implicitTypeRef
    }
}
