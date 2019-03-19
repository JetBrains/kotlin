/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedFunctionTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirSpecificTypeResolverTransformer(
    private val towerScope: FirScope,
    private val position: FirPosition,
    private val session: FirSession
) : FirAbstractTreeTransformer() {
    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        val typeResolver = FirTypeResolver.getInstance(session)
        typeRef.transformChildren(FirSpecificTypeResolverTransformer(towerScope, FirPosition.OTHER, session), null)
        return transformType(typeRef, typeResolver.resolveType(typeRef, towerScope, position))
    }

    override fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        val typeResolver = FirTypeResolver.getInstance(session)
        functionTypeRef.transformChildren(this, data)
        return FirResolvedFunctionTypeRefImpl(
            functionTypeRef.psi,
            session,
            functionTypeRef.isMarkedNullable,
            functionTypeRef.annotations as MutableList<FirAnnotationCall>,
            functionTypeRef.receiverTypeRef,
            functionTypeRef.valueParameters as MutableList<FirValueParameter>,
            functionTypeRef.returnTypeRef,
            typeResolver.resolveType(functionTypeRef, towerScope, position)
        ).compose()
    }

    private fun transformType(typeRef: FirTypeRef, resolvedType: ConeKotlinType): CompositeTransformResult<FirTypeRef> {
        return FirResolvedTypeRefImpl(
            session,
            typeRef.psi,
            resolvedType,
            false,
            typeRef.annotations
        ).compose()
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return resolvedTypeRef.compose()
    }
}
