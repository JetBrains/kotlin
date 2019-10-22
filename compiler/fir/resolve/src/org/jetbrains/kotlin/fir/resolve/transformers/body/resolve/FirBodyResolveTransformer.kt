/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.FqName

open class FirBodyResolveTransformer(
    session: FirSession,
    phase: FirResolvePhase,
    override var implicitTypeOnly: Boolean,
    scopeSession: ScopeSession
) : FirAbstractBodyResolveTransformer(phase) {
    private var packageFqName = FqName.ROOT

    override val components: BodyResolveTransformerComponents = BodyResolveTransformerComponents(session, scopeSession, this)

    private val expressionsTransformer = FirExpressionsResolveTransformer(this)
    private val declarationsTransformer = FirDeclarationsResolveTransformer(this)
    private val controlFlowStatementsTransformer = FirControlFlowStatementsResolveTransformer(this)

    override fun transformFile(file: FirFile, data: Any?): CompositeTransformResult<FirFile> {
        packageFqName = file.packageFqName
        components.file = file
        return withScopeCleanup(components.topLevelScopes) {
            components.topLevelScopes.addImportingScopes(file, session, components.scopeSession)
            @Suppress("UNCHECKED_CAST")
            super.transformFile(file, data) as CompositeTransformResult<FirFile>
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Any?): CompositeTransformResult<FirTypeRef> {
        return typeRef.compose()
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Any?): CompositeTransformResult<FirTypeRef> {
        if (data == null)
            return implicitTypeRef.compose()
        require(data is FirTypeRef)
        return data.compose()
    }

    // ------------------------------------- Expressions -------------------------------------

    override fun transformExpression(expression: FirExpression, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformExpression(expression, data)
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformQualifiedAccessExpression(qualifiedAccessExpression, data)
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformFunctionCall(functionCall, data)
    }

    override fun transformBlock(block: FirBlock, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformBlock(block, data)
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformThisReceiverExpression(thisReceiverExpression, data)
    }

    override fun transformOperatorCall(operatorCall: FirOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformOperatorCall(operatorCall, data)
    }

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformTypeOperatorCall(typeOperatorCall, data)
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformBinaryLogicExpression(binaryLogicExpression, data)
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformVariableAssignment(variableAssignment, data)
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformGetClassCall(getClassCall, data)
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformWrappedDelegateExpression(wrappedDelegateExpression, data)
    }

    override fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformConstExpression(constExpression, data)
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformAnnotationCall(annotationCall, data)
    }

    // ------------------------------------- Declarations -------------------------------------

    override fun transformDeclaration(declaration: FirDeclaration, data: Any?): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformDeclaration(declaration, data)
    }

    override fun transformProperty(property: FirProperty, data: Any?): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformProperty(property, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformRegularClass(regularClass, data)
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformSimpleFunction(simpleFunction, data)
    }

    override fun <F : FirFunction<F>> transformFunction(function: FirFunction<F>, data: Any?): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformFunction(function, data)
    }

    override fun transformConstructor(constructor: FirConstructor, data: Any?): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformConstructor(constructor, data)
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: Any?
    ): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformAnonymousInitializer(anonymousInitializer, data)
    }

    override fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformAnonymousFunction(anonymousFunction, data)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Any?): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformValueParameter(valueParameter, data)
    }

    // ------------------------------------- Control flow statements -------------------------------------

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: Any?): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformWhileLoop(whileLoop, data)
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Any?): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformDoWhileLoop(doWhileLoop, data)
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformWhenExpression(whenExpression, data)
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: Any?): CompositeTransformResult<FirWhenBranch> {
        return controlFlowStatementsTransformer.transformWhenBranch(whenBranch, data)
    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformWhenSubjectExpression(whenSubjectExpression, data)
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: Any?): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformTryExpression(tryExpression, data)
    }

    override fun transformCatch(catch: FirCatch, data: Any?): CompositeTransformResult<FirCatch> {
        return controlFlowStatementsTransformer.transformCatch(catch, data)
    }

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: Any?): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformJump(jump, data)
    }

    override fun transformThrowExpression(throwExpression: FirThrowExpression, data: Any?): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformThrowExpression(throwExpression, data)
    }

    // --------------------------------------------------------------------------

    fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform<FirElement, D>(transformer, data)
        require(result.single === this) { "become ${result.single}: `${result.single.render()}`, was ${this}: `${this.render()}`" }
    }
}