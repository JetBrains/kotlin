/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirSealedClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.types.*

abstract class FirDefaultVisitorVoid : FirVisitorVoid() {
    override fun visitDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef) {
        return visitTypeRef(delegatedTypeRef)
    }

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
        return visitTypeRef(implicitTypeRef)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        return visitTypeRef(resolvedTypeRef)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        return visitResolvedTypeRef(errorTypeRef)
    }

    override fun visitResolvedFunctionTypeRef(resolvedFunctionTypeRef: FirResolvedFunctionTypeRef) {
        return visitResolvedTypeRef(resolvedFunctionTypeRef)
    }

    override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability) {
        return visitTypeRef(typeRefWithNullability)
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef) {
        return visitTypeRefWithNullability(dynamicTypeRef)
    }

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef) {
        return visitTypeRefWithNullability(functionTypeRef)
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
        return visitTypeRefWithNullability(userTypeRef)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
        return visitQualifiedAccessExpression(callableReferenceAccess)
    }

    override fun visitComponentCall(componentCall: FirComponentCall) {
        return visitFunctionCall(componentCall)
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression) {
        return visitJump(returnExpression)
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression) {
        return visitJump(continueExpression)
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression) {
        return visitJump(breakExpression)
    }


    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression) {
        return visitWrappedArgumentExpression(lambdaArgumentExpression)
    }

    override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression) {
        return visitWrappedArgumentExpression(spreadArgumentExpression)
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression) {
        return visitWrappedArgumentExpression(namedArgumentExpression)
    }

    override fun visitSealedClass(sealedClass: FirSealedClass) {
        return visitRegularClass(sealedClass)
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression) {
        return visitExpression(errorExpression)
    }

    override fun visitErrorLoop(errorLoop: FirErrorLoop) {
        return visitLoop(errorLoop)
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference) {
        return visitNamedReference(errorNamedReference)
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction) {
        return visitFunction(errorFunction)
    }
}