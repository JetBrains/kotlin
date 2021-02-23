/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.fakeElement
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

internal object StoreType : FirDefaultTransformer<FirTypeRef>() {
    override fun <E : FirElement> transformElement(element: E, data: FirTypeRef): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: FirTypeRef): CompositeTransformResult<FirTypeRef> {
        return data.compose()
    }
}

internal object TransformImplicitType : FirDefaultTransformer<FirTypeRef>() {
    override fun <E : FirElement> transformElement(element: E, data: FirTypeRef): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformImplicitTypeRef(
        implicitTypeRef: FirImplicitTypeRef,
        data: FirTypeRef
    ): CompositeTransformResult<FirTypeRef> {
        return data.compose()
    }
}


internal object StoreNameReference : FirDefaultTransformer<FirNamedReference>() {
    override fun <E : FirElement> transformElement(element: E, data: FirNamedReference): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformNamedReference(
        namedReference: FirNamedReference,
        data: FirNamedReference
    ): CompositeTransformResult<FirNamedReference> {
        return data.compose()
    }

    override fun transformThisReference(thisReference: FirThisReference, data: FirNamedReference): CompositeTransformResult<FirReference> {
        return data.compose()
    }

    override fun transformSuperReference(
        superReference: FirSuperReference,
        data: FirNamedReference
    ): CompositeTransformResult<FirReference> {
        return data.compose()
    }
}

internal object StoreCalleeReference : FirTransformer<FirNamedReference>() {
    override fun <E : FirElement> transformElement(element: E, data: FirNamedReference): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformNamedReference(
        namedReference: FirNamedReference,
        data: FirNamedReference
    ): CompositeTransformResult<FirNamedReference> {
        return data.compose()
    }

    override fun transformResolvedNamedReference(
        resolvedNamedReference: FirResolvedNamedReference,
        data: FirNamedReference
    ): CompositeTransformResult<FirNamedReference> {
        return data.compose()
    }
}

internal object StoreReceiver : FirTransformer<FirExpression>() {
    override fun <E : FirElement> transformElement(element: E, data: FirExpression): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (data as E).compose()
    }
}

internal fun FirValueParameter.transformVarargTypeToArrayType() {
    if (isVararg) {
        this.transformTypeToArrayType()
    }
}

internal fun FirTypedDeclaration.transformTypeToArrayType() {
    val returnTypeRef = this.returnTypeRef
    require(returnTypeRef is FirResolvedTypeRef)
    // If the delegated type is already resolved, it means we have already created a resolved array type for this vararg type declaration.
    // This is because in the buildResolvedTypeRef call below, we set the delegated type ref to the previous (non-vararg) resolved type ref.
    if (returnTypeRef.delegatedTypeRef is FirResolvedTypeRef &&
        returnTypeRef.delegatedTypeRef?.source?.kind == FirFakeSourceElementKind.ArrayTypeFromVarargParameter
    ) return
    val returnType = returnTypeRef.coneType

    transformReturnTypeRef(
        StoreType,
        buildResolvedTypeRef {
            source = returnTypeRef.source
            type = ConeKotlinTypeProjectionOut(returnType).createArrayType()
            annotations += returnTypeRef.annotations
            delegatedTypeRef = returnTypeRef.apply {
                // Here we replace the source of the immediate delegate and nested delegate with a fake source. Normally the immediate
                // delegate is the original resolved type ref before this array augmentation. That is, this is the array element type.
                // The nested delegate is the raw type ref created when constructing the FIR. Usually it's a `FirUserTypeRef` for value
                // parameters.
                replaceSource(source?.fakeElement(FirFakeSourceElementKind.ArrayTypeFromVarargParameter))
                delegatedTypeRef?.replaceSource(source?.fakeElement(FirFakeSourceElementKind.ArrayTypeFromVarargParameter))
            }
        }
    )
}

inline fun <T> withScopeCleanup(scopes: MutableList<*>, crossinline l: () -> T): T {
    val sizeBefore = scopes.size
    return try {
        l()
    } finally {
        val size = scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            scopes.let { it.removeAt(it.size - 1) }
        }
    }
}
