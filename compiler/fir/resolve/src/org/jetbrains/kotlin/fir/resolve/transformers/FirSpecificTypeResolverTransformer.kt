/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedFunctionTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirSpecificTypeResolverTransformer(
    private val towerScope: FirScope,
    override val session: FirSession
) : FirAbstractTreeTransformer<Nothing?>(phase = FirResolvePhase.SUPER_TYPES) {
    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        val typeResolver = FirTypeResolver.getInstance(session)
        typeRef.transformChildren(FirSpecificTypeResolverTransformer(towerScope, session), null)
        return transformType(typeRef, typeResolver.resolveType(typeRef, towerScope))
    }

    override fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        val typeResolver = FirTypeResolver.getInstance(session)
        functionTypeRef.transformChildren(this, data)
        return FirResolvedFunctionTypeRefImpl(
            functionTypeRef.source,
            typeResolver.resolveType(functionTypeRef, towerScope),
            functionTypeRef.isMarkedNullable,
            functionTypeRef.receiverTypeRef,
            functionTypeRef.returnTypeRef
        ).apply {
            annotations += functionTypeRef.annotations
            valueParameters += functionTypeRef.valueParameters
        }.compose()
    }

    private fun transformType(typeRef: FirTypeRef, resolvedType: ConeKotlinType): CompositeTransformResult<FirTypeRef> {
        return FirResolvedTypeRefImpl(
            typeRef.source,
            resolvedType
        ).apply {
            annotations += typeRef.annotations
        }.compose()
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return resolvedTypeRef.compose()
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return implicitTypeRef.compose()
    }
}
