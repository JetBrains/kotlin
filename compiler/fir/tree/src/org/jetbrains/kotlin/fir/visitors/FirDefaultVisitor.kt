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

abstract class FirDefaultVisitor<R, D> : FirVisitor<R, D>() {
    override fun visitDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef, data: D): R {
        return visitTypeRef(delegatedTypeRef, data)
    }

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): R {
        return visitTypeRef(implicitTypeRef, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): R {
        return visitTypeRef(resolvedTypeRef, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): R {
        return visitResolvedTypeRef(errorTypeRef, data)
    }

    override fun visitResolvedFunctionTypeRef(resolvedFunctionTypeRef: FirResolvedFunctionTypeRef, data: D): R {
        return visitResolvedTypeRef(resolvedFunctionTypeRef, data)
    }

    override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): R {
        return visitTypeRef(typeRefWithNullability, data)
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): R {
        return visitTypeRefWithNullability(dynamicTypeRef, data)
    }

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): R {
        return visitTypeRefWithNullability(functionTypeRef, data)
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: D): R {
        return visitTypeRefWithNullability(userTypeRef, data)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): R {
        return visitQualifiedAccessExpression(callableReferenceAccess, data)
    }

    override fun visitComponentCall(componentCall: FirComponentCall, data: D): R {
        return visitFunctionCall(componentCall, data)
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: D): R {
        return visitJump(returnExpression, data)
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: D): R {
        return visitJump(continueExpression, data)
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: D): R {
        return visitJump(breakExpression, data)
    }


    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): R {
        return visitWrappedArgumentExpression(lambdaArgumentExpression, data)
    }

    override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): R {
        return visitWrappedArgumentExpression(spreadArgumentExpression, data)
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): R {
        return visitWrappedArgumentExpression(namedArgumentExpression, data)
    }

    override fun visitSealedClass(sealedClass: FirSealedClass, data: D): R {
        return visitRegularClass(sealedClass, data)
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: D): R {
        return visitExpression(errorExpression, data)
    }

    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: D): R {
        return visitLoop(errorLoop, data)
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): R {
        return visitNamedReference(errorNamedReference, data)
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction, data: D): R {
        return visitFunction(errorFunction, data)
    }
}

