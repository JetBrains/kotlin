/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedFunctionTypeRef
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

    override fun transformTypeRef(typeRef: FirTypeRef, data: FirScope): CompositeTransformResult<FirTypeRef> {
        typeRef.transformChildren(this, data)
        return transformType(typeRef, typeResolver.resolveType(typeRef, data, areBareTypesAllowed))
    }

    override fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: FirScope): CompositeTransformResult<FirTypeRef> {
        functionTypeRef.transformChildren(this, data)
        return buildResolvedFunctionTypeRef {
            source = functionTypeRef.source
            type = typeResolver.resolveType(functionTypeRef, data, areBareTypesAllowed).takeIfAcceptable() ?: return functionTypeRef.compose()
            isMarkedNullable = functionTypeRef.isMarkedNullable
            isSuspend = functionTypeRef.isSuspend
            receiverTypeRef = functionTypeRef.receiverTypeRef
            returnTypeRef = functionTypeRef.returnTypeRef
            annotations += functionTypeRef.annotations
            valueParameters += functionTypeRef.valueParameters
        }.compose()
    }

    private fun transformType(typeRef: FirTypeRef, resolvedType: ConeKotlinType): CompositeTransformResult<FirTypeRef> {
        return if (resolvedType !is ConeClassErrorType) {
            buildResolvedTypeRef {
                source = typeRef.source
                type = resolvedType.takeIfAcceptable() ?: return typeRef.compose()
                annotations += typeRef.annotations
                delegatedTypeRef = typeRef
            }
        } else {
            buildErrorTypeRef {
                source = resolvedType.diagnostic.safeAs<ConeUnexpectedTypeArgumentsError>()
                    ?.source.safeAs()
                    ?: typeRef.source
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
