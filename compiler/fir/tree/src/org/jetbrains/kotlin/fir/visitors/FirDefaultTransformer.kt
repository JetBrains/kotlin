/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirSealedClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.*

abstract class FirDefaultTransformer<D> : FirTransformer<D>() {
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

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: D
    ): CompositeTransformResult<FirStatement> {
        return transformQualifiedAccessExpression(callableReferenceAccess, data)
    }

    override fun transformComponentCall(componentCall: FirComponentCall, data: D): CompositeTransformResult<FirStatement> {
        return transformFunctionCall(componentCall, data)
    }

    override fun transformReturnExpression(returnExpression: FirReturnExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformJump(returnExpression, data)
    }

    override fun transformContinueExpression(continueExpression: FirContinueExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformJump(continueExpression, data)
    }

    override fun transformBreakExpression(breakExpression: FirBreakExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformJump(breakExpression, data)
    }


    override fun transformLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWrappedArgumentExpression(lambdaArgumentExpression, data)
    }

    override fun transformSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWrappedArgumentExpression(spreadArgumentExpression, data)
    }

    override fun transformNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWrappedArgumentExpression(namedArgumentExpression, data)
    }

    override fun transformSealedClass(sealedClass: FirSealedClass, data: D): CompositeTransformResult<FirStatement> {
        return transformRegularClass(sealedClass, data)
    }

    override fun transformErrorExpression(errorExpression: FirErrorExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformExpression(errorExpression, data)
    }

    override fun transformErrorLoop(errorLoop: FirErrorLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformLoop(errorLoop, data)
    }

    override fun transformErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): CompositeTransformResult<FirReference> {
        return transformNamedReference(errorNamedReference, data)
    }

    override fun transformErrorFunction(errorFunction: FirErrorFunction, data: D): CompositeTransformResult<FirStatement> {
        return transformFunction(errorFunction, data)
    }
}

