/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

abstract class FirTypeTransformer<D> : FirTransformer<D>() {
    override fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(typeRef, data)
    }

    override fun transformDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(delegatedTypeRef, data)
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(implicitTypeRef, data)
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(resolvedTypeRef, data)
    }

    override fun transformErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformResolvedTypeRef(errorTypeRef, data)
    }

    override fun transformResolvedFunctionTypeRef(resolvedFunctionTypeRef: FirResolvedFunctionTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformResolvedTypeRef(resolvedFunctionTypeRef, data)
    }

    override fun transformTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(typeRefWithNullability, data)
    }

    override fun transformDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRefWithNullability(dynamicTypeRef, data)
    }

    override fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRefWithNullability(functionTypeRef, data)
    }

    override fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRefWithNullability(userTypeRef, data)
    }


}