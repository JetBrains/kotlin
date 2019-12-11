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
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
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

    override fun transformFile(file: FirFile, data: ResolutionMode): CompositeTransformResult<FirFile> {
        packageFqName = file.packageFqName
        components.file = file
        return withScopeCleanup(components.topLevelScopes) {
            components.topLevelScopes.addImportingScopes(file, session, components.scopeSession)
            super.transformFile(file, data)
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: ResolutionMode): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: ResolutionMode): CompositeTransformResult<FirTypeRef> {
        if (typeRef is FirResolvedTypeRef) {
            return typeRef.compose()
        }
        return typeResolverTransformer.transformTypeRef(typeRef, null)
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: ResolutionMode): CompositeTransformResult<FirTypeRef> {
        if (data !is ResolutionMode.WithExpectedType)
            return implicitTypeRef.compose()
        return data.expectedTypeRef.compose()
    }

    // ------------------------------------- Expressions -------------------------------------

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformExpression(expression, data)
    }

    override fun transformWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return (wrappedArgumentExpression.transformChildren(this, data) as FirStatement).compose()
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformQualifiedAccessExpression(qualifiedAccessExpression, data)
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformFunctionCall(functionCall, data)
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformCallableReferenceAccess(callableReferenceAccess, data)
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformBlock(block, data)
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformThisReceiverExpression(thisReceiverExpression, data)
    }

    override fun transformOperatorCall(operatorCall: FirOperatorCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformOperatorCall(operatorCall, data)
    }

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformTypeOperatorCall(typeOperatorCall, data)
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformCheckNotNullCall(checkNotNullCall, data)
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformBinaryLogicExpression(binaryLogicExpression, data)
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformVariableAssignment(variableAssignment, data)
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformGetClassCall(getClassCall, data)
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformWrappedDelegateExpression(wrappedDelegateExpression, data)
    }

    override fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformConstExpression(constExpression, data)
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformAnnotationCall(annotationCall, data)
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return expressionsTransformer.transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }

    // ------------------------------------- Declarations -------------------------------------

    override fun transformDeclaration(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformDeclaration(declaration, data)
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclarationStatus> {
        return declarationsTransformer.transformDeclarationStatus(declarationStatus, data)
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformProperty(property, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformRegularClass(regularClass, data)
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformAnonymousObject(anonymousObject, data)
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformSimpleFunction(simpleFunction, data)
    }

    override fun <F : FirFunction<F>> transformFunction(function: FirFunction<F>, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformFunction(function, data)
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformConstructor(constructor, data)
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        return declarationsTransformer.transformAnonymousInitializer(anonymousInitializer, data)
    }

    override fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformAnonymousFunction(anonymousFunction, data)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return declarationsTransformer.transformValueParameter(valueParameter, data)
    }

    // ------------------------------------- Control flow statements -------------------------------------

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformWhileLoop(whileLoop, data)
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformDoWhileLoop(doWhileLoop, data)
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformWhenExpression(whenExpression, data)
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: ResolutionMode): CompositeTransformResult<FirWhenBranch> {
        return controlFlowStatementsTransformer.transformWhenBranch(whenBranch, data)
    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformWhenSubjectExpression(whenSubjectExpression, data)
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformTryExpression(tryExpression, data)
    }

    override fun transformCatch(catch: FirCatch, data: ResolutionMode): CompositeTransformResult<FirCatch> {
        return controlFlowStatementsTransformer.transformCatch(catch, data)
    }

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformJump(jump, data)
    }

    override fun transformThrowExpression(throwExpression: FirThrowExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return controlFlowStatementsTransformer.transformThrowExpression(throwExpression, data)
    }

    // --------------------------------------------------------------------------

    fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform<FirElement, D>(transformer, data)
        require(result.single === this) { "become ${result.single}: `${result.single.render()}`, was ${this}: `${this.render()}`" }
    }
}
