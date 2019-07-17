/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.compare

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractCallableMember
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractTreeTransformer
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

class FirPartialTransformer(
    private val visitAnonymousFunction: Boolean = false,
    private val visitLambdaExpression: Boolean = false,
    private val visitNamedFunction: Boolean = false,
    private val visitMemberDeclaration: Boolean = false,
    private val visitVariable: Boolean = false,
    private val visitAnnotation: Boolean = false,
    private val visitTypeOperatorCall: Boolean = false,
    private val visitArrayOfCall: Boolean = false,
    private val visitFunctionCall: Boolean = false,
    private val visitGetClassCall: Boolean = false,
    private val visitBreakExpression: Boolean = false,
    private val visitContinueExpression: Boolean = false,
    private val visitReturnExpression: Boolean = false,
    private val visitThrowExpression: Boolean = false,
    private val visitForLoop: Boolean = false,
    private val visitClassReferenceExpression: Boolean = false,
    private val visitConstExpression: Boolean = false,
    private val visitQualifiedAccessExpression: Boolean = false,
    private val visitCallableReferenceAccess: Boolean = false,
    private val visitTryExpression: Boolean = false,
    private val visitWhenExpression: Boolean = false,
    private val visitNamedArgumentExpression: Boolean = false,
    private val visitLambdaArgumentExpression: Boolean = false,
    private val visitSpreadArgumentExpression: Boolean = false,
    private val visitAnonymousObject: Boolean = false,
    private val visitDoWhileLoop: Boolean = false,
    private val visitWhileLoop: Boolean = false,
    private val visitAssignment: Boolean = false
) : FirAbstractTreeTransformer() {
    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return if (visitAnonymousFunction) {
            (anonymousFunction.transformChildren(this, data) as FirAnonymousFunction).compose()
        } else if (visitLambdaExpression && anonymousFunction.psi is KtLambdaExpression) {
            (anonymousFunction.transformChildren(this, data) as FirAnonymousFunction).compose()
        } else {
            DummyFirAnonymousFunction().compose()
        }
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return if (!visitNamedFunction && namedFunction is FirAbstractCallableMember && namedFunction.visibility == Visibilities.LOCAL) {
            DummyFirDeclaration().compose()
        } else {
            (namedFunction.transformChildren(this, data) as FirNamedFunction).compose()
        }
    }

    override fun transformMemberDeclaration(
        memberDeclaration: FirMemberDeclaration,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return if (!visitMemberDeclaration && memberDeclaration.visibility == Visibilities.LOCAL) {
            DummyFirDeclaration().compose()
        } else {
            (memberDeclaration.transformChildren(this, data) as FirMemberDeclaration).compose()
        }
    }

    override fun transformVariable(variable: FirVariable, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return if (visitVariable) {
            (variable.transformChildren(this, data) as FirVariable).compose()
        } else {
            DummyFirVariable().compose()
        }
    }

    override fun transformExpression(expression: FirExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (expression is FirExpressionStub) {
            DummyFirStatement().compose()
        } else {
            super.transformExpression(expression, data)
        }
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Nothing?): CompositeTransformResult<FirAnnotationCall> {
        return if (visitAnnotation) {
            (annotationCall.transformChildren(this, data) as FirAnnotationCall).compose()
        } else {
            CompositeTransformResult.empty()
        }
    }

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitTypeOperatorCall) {
            (typeOperatorCall.transformChildren(this, data) as FirTypeOperatorCall).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitArrayOfCall) {
            (arrayOfCall.transformChildren(this, data) as FirArrayOfCall).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitFunctionCall) {
            (functionCall.transformChildren(this, data) as FirFunctionCall).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitGetClassCall) {
            (getClassCall.transformChildren(this, data) as FirGetClassCall).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformBreakExpression(breakExpression: FirBreakExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitBreakExpression) {
            (breakExpression.transformChildren(this, data) as FirBreakExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformContinueExpression(
        continueExpression: FirContinueExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitContinueExpression) {
            (continueExpression.transformChildren(this, data) as FirContinueExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformReturnExpression(returnExpression: FirReturnExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitReturnExpression) {
            (returnExpression.transformChildren(this, data) as FirReturnExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformThrowExpression(throwExpression: FirThrowExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitThrowExpression) {
            (throwExpression.transformChildren(this, data) as FirThrowExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformBlock(block: FirBlock, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (!visitForLoop && block.psi is KtForExpression) {
            DummyFirStatement().compose()
        } else {
            super.transformBlock(block, data)
        }
    }

    override fun transformClassReferenceExpression(
        classReferenceExpression: FirClassReferenceExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitClassReferenceExpression) {
            (classReferenceExpression.transformChildren(this, data) as FirClassReferenceExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitConstExpression) {
            (constExpression.transformChildren(this, data) as FirConstExpression<*>).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitQualifiedAccessExpression) {
            (qualifiedAccessExpression.transformChildren(this, data) as FirQualifiedAccessExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitCallableReferenceAccess) {
            (callableReferenceAccess.transformChildren(this, data) as FirCallableReferenceAccess).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitTryExpression) {
            (tryExpression.transformChildren(this, data) as FirTryExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitWhenExpression) {
            (whenExpression.transformChildren(this, data) as FirWhenExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformNamedArgumentExpression(
        namedArgumentExpression: FirNamedArgumentExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitNamedArgumentExpression) {
            (namedArgumentExpression.transformChildren(this, data) as FirNamedArgumentExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformLambdaArgumentExpression(
        lambdaArgumentExpression: FirLambdaArgumentExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitLambdaArgumentExpression) {
            (lambdaArgumentExpression.transformChildren(this, data) as FirLambdaArgumentExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformSpreadArgumentExpression(
        spreadArgumentExpression: FirSpreadArgumentExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return if (visitSpreadArgumentExpression) {
            (spreadArgumentExpression.transformChildren(this, data) as FirSpreadArgumentExpression).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitAnonymousObject) {
            (anonymousObject.transformChildren(this, data) as FirAnonymousObject).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitDoWhileLoop) {
            (doWhileLoop.transformChildren(this, data) as FirDoWhileLoop).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitWhileLoop) {
            (whileLoop.transformChildren(this, data) as FirWhileLoop).compose()
        } else {
            DummyFirStatement().compose()
        }
    }

    override fun transformAssignment(assignment: FirAssignment, data: Nothing?): CompositeTransformResult<FirStatement> {
        return if (visitAssignment) {
            (assignment.transformChildren(this, data) as FirAssignment).compose()
        } else {
            DummyFirStatement().compose()
        }
    }
}