/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.*

internal object MapArguments : FirDefaultTransformer<Map<FirElement, FirElement>>() {
    override fun <E : FirElement> transformElement(element: E, data: Map<FirElement, FirElement>): CompositeTransformResult<E> {
        return ((data[element] ?: element) as E).compose()
    }

    override fun transformFunctionCall(
        functionCall: FirFunctionCall,
        data: Map<FirElement, FirElement>
    ): CompositeTransformResult<FirStatement> {
        return (functionCall.transformArguments(this, data) as FirStatement).compose()
    }

    override fun transformWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: Map<FirElement, FirElement>
    ): CompositeTransformResult<FirStatement> {
        return (wrappedArgumentExpression.transformChildren(this, data) as FirStatement).compose()
    }

    override fun transformBlock(block: FirBlock, data: Map<FirElement, FirElement>): CompositeTransformResult<FirStatement> {
        if (block.statements.isEmpty()) return block.compose()
        val transformedStatement = block.statements.last().transformSingle(this@MapArguments, data)
        when (block) {
            is FirSingleExpressionBlock -> block.statement = transformedStatement
            // TODO: dirty cast
            else -> (block.statements as MutableList<FirStatement>)[block.statements.size - 1] = transformedStatement
        }
        return block.compose()
    }

    override fun transformCatch(catch: FirCatch, data: Map<FirElement, FirElement>): CompositeTransformResult<FirCatch> {
        return catch.transformBlock(this, data).compose()
    }

    override fun transformWhenBranch(
        whenBranch: FirWhenBranch,
        data: Map<FirElement, FirElement>,
    ): CompositeTransformResult<FirWhenBranch> {
        return  whenBranch.transformResult(this, data).compose()
    }
}

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
        val returnType = returnTypeRef.coneTypeUnsafe<ConeKotlinType>()
        transformReturnTypeRef(
            StoreType,
            returnTypeRef.withReplacedConeType(returnType.createArrayOf(session))
        )
    }
}