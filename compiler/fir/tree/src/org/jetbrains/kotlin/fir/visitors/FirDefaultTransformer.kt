/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.declarations.FirConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.*

abstract class FirDefaultTransformer<D> : FirTransformer<D>() {
    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): FirTypeRef {
        return transformTypeRef(implicitTypeRef, data)
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): FirTypeRef {
        return transformTypeRef(resolvedTypeRef, data)
    }

    override fun transformErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): FirTypeRef {
        return transformResolvedTypeRef(errorTypeRef, data)
    }

    override fun transformTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): FirTypeRef {
        return transformTypeRef(typeRefWithNullability, data)
    }

    override fun transformDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): FirTypeRef {
        return transformTypeRefWithNullability(dynamicTypeRef, data)
    }

    override fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): FirTypeRef {
        return transformTypeRefWithNullability(functionTypeRef, data)
    }

    override fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: D): FirTypeRef {
        return transformTypeRefWithNullability(userTypeRef, data)
    }

    override fun transformIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: D): FirTypeRef {
        return transformTypeRef(intersectionTypeRef, data)
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: D
    ): FirStatement {
        return transformQualifiedAccessExpression(callableReferenceAccess, data)
    }

    override fun transformComponentCall(componentCall: FirComponentCall, data: D): FirStatement {
        return transformFunctionCall(componentCall, data)
    }

    override fun transformReturnExpression(returnExpression: FirReturnExpression, data: D): FirStatement {
        return transformJump(returnExpression, data)
    }

    override fun transformContinueExpression(continueExpression: FirContinueExpression, data: D): FirStatement {
        return transformJump(continueExpression, data)
    }

    override fun transformBreakExpression(breakExpression: FirBreakExpression, data: D): FirStatement {
        return transformJump(breakExpression, data)
    }


    override fun transformLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): FirStatement {
        return transformWrappedArgumentExpression(lambdaArgumentExpression, data)
    }

    override fun transformSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): FirStatement {
        return transformWrappedArgumentExpression(spreadArgumentExpression, data)
    }

    override fun transformNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): FirStatement {
        return transformWrappedArgumentExpression(namedArgumentExpression, data)
    }

    override fun transformErrorExpression(errorExpression: FirErrorExpression, data: D): FirStatement {
        return transformExpression(errorExpression, data)
    }

    override fun transformErrorLoop(errorLoop: FirErrorLoop, data: D): FirStatement {
        return transformLoop(errorLoop, data)
    }

    override fun transformErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): FirReference {
        return transformNamedReference(errorNamedReference, data)
    }

    override fun transformErrorFunction(errorFunction: FirErrorFunction, data: D): FirStatement {
        return transformFunction(errorFunction, data)
    }

    override fun transformErrorResolvedQualifier(
        errorResolvedQualifier: FirErrorResolvedQualifier,
        data: D
    ): FirStatement {
        return transformResolvedQualifier(errorResolvedQualifier, data)
    }

    override fun transformImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: D): FirStatement {
        return transformFunctionCall(implicitInvokeCall, data)
    }

    override fun transformConstructedClassTypeParameterRef(constructedClassTypeParameterRef: FirConstructedClassTypeParameterRef, data: D): FirTypeParameterRef {
        return transformTypeParameterRef(constructedClassTypeParameterRef, data)
    }

    override fun transformOuterClassTypeParameterRef(outerClassTypeParameterRef: FirOuterClassTypeParameterRef, data: D): FirTypeParameterRef {
        return transformTypeParameterRef(outerClassTypeParameterRef, data)
    }
}

