/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.createCurrentScopeList
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

abstract class FirAbstractBodyResolveTransformerDispatcher(
    session: FirSession,
    phase: FirResolvePhase,
    override var implicitTypeOnly: Boolean,
    scopeSession: ScopeSession,
    val returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve,
    outerBodyResolveContext: BodyResolveContext? = null,
    val firTowerDataContextCollector: FirTowerDataContextCollector? = null,
) : FirAbstractBodyResolveTransformer(phase) {

    open val preserveCFGForClasses: Boolean get() = !implicitTypeOnly

    final override val context: BodyResolveContext =
        outerBodyResolveContext ?: BodyResolveContext(returnTypeCalculator, DataFlowAnalyzerContext(session))
    final override val components: BodyResolveTransformerComponents =
        BodyResolveTransformerComponents(session, scopeSession, this, context)

    final override val resolutionContext: ResolutionContext = ResolutionContext(session, components, context)

    abstract val expressionsTransformer: FirExpressionsResolveTransformer

    abstract val declarationsTransformer: FirDeclarationsResolveTransformer

    private val controlFlowStatementsTransformer = FirControlFlowStatementsResolveTransformer(this)

    override fun transformFile(file: FirFile, data: ResolutionMode): FirFile {
        checkSessionConsistency(file)
        return context.withFile(file, components) {
            withFileAnalysisExceptionWrapping(file) {
                firTowerDataContextCollector?.addFileContext(file, context.towerDataContext)

                @Suppress("UNCHECKED_CAST")
                transformDeclarationContent(file, data) as FirFile
            }
        }
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
        return declarationsTransformer.transformScript(script, data)
    }

    override fun <E : FirElement> transformElement(element: E, data: ResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: ResolutionMode): FirResolvedTypeRef {
        val resolvedTypeRef = if (typeRef is FirResolvedTypeRef) {
            typeRef
        } else {
            typeResolverTransformer.withFile(context.file) {
                transformTypeRef(
                    typeRef,
                    ScopeClassDeclaration(
                        components.createCurrentScopeList(),
                        context.containingClassDeclarations,
                        context.containers.lastOrNull { it is FirTypeParameterRefsOwner && it !is FirAnonymousFunction }
                    )
                )
            }
        }

        resolvedTypeRef.coneType.forEachType {
            it.type.attributes.customAnnotations.forEach { typeArgumentAnnotation ->
                typeArgumentAnnotation.accept(this, data)
            }
        }

        return resolvedTypeRef.transformAnnotations(this, data)
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: ResolutionMode): FirTypeRef {
        if (data !is ResolutionMode.WithExpectedType)
            return implicitTypeRef
        return data.expectedTypeRef
    }

    // ------------------------------------- Expressions -------------------------------------

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformExpression(expression, data)
    }

    override fun transformWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: ResolutionMode
    ): FirStatement {
        return transformElement(wrappedArgumentExpression, data)
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformQualifiedAccessExpression(qualifiedAccessExpression, data)
    }

    override fun transformPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformFunctionCall(functionCall, data)
    }

    override fun transformStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformStringConcatenationCall(stringConcatenationCall, data)
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformCallableReferenceAccess(callableReferenceAccess, data)
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformBlock(block, data)
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformThisReceiverExpression(thisReceiverExpression, data)
    }

    override fun transformComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformComparisonExpression(comparisonExpression, data)
    }

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformTypeOperatorCall(typeOperatorCall, data)
    }

    override fun transformAssignmentOperatorStatement(
        assignmentOperatorStatement: FirAssignmentOperatorStatement,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformAssignmentOperatorStatement(assignmentOperatorStatement, data)
    }

    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: FirIncrementDecrementExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformIncrementDecrementExpression(incrementDecrementExpression, data)
    }

    override fun transformEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformEqualityOperatorCall(equalityOperatorCall, data)
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformCheckNotNullCall(checkNotNullCall, data)
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformBinaryLogicExpression(binaryLogicExpression, data)
    }

    override fun transformDesugaredAssignmentValueReferenceExpression(
        desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformDesugaredAssignmentValueReferenceExpression(
            desugaredAssignmentValueReferenceExpression,
            data,
        )
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformVariableAssignment(variableAssignment, data)
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformGetClassCall(getClassCall, data)
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode
    ): FirStatement {
        return declarationsTransformer.transformWrappedDelegateExpression(wrappedDelegateExpression, data)
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformConstExpression(constExpression, data)
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformAnnotation(annotation, data)
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformAnnotationCall(annotationCall, data)
    }

    override fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformErrorAnnotationCall(errorAnnotationCall, data)
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }

    override fun transformAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformAugmentedArraySetCall(augmentedArraySetCall, data)
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformSafeCallExpression(safeCallExpression, data)
    }

    override fun transformCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformCheckedSafeCallSubject(checkedSafeCallSubject, data)
    }

    override fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformArrayOfCall(arrayOfCall, data)
    }

    override fun transformSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: ResolutionMode): FirStatement {
        return expressionsTransformer.transformSmartCastExpression(smartCastExpression, data)
    }

    // ------------------------------------- Declarations -------------------------------------

    override fun transformDeclaration(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration {
        return declarationsTransformer.transformDeclaration(declaration, data)
    }

    open fun transformDeclarationContent(
        declaration: FirDeclaration, data: ResolutionMode
    ): FirDeclaration {
        return transformElement(declaration, data)
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: ResolutionMode
    ): FirDeclarationStatus {
        return declarationsTransformer.transformDeclarationStatus(declarationStatus, data)
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirEnumEntry {
        return declarationsTransformer.transformEnumEntry(enumEntry, data)
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        return declarationsTransformer.transformProperty(property, data)
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: ResolutionMode): FirStatement {
        return declarationsTransformer.transformPropertyAccessor(propertyAccessor, data)
    }

    override fun transformBackingField(
        backingField: FirBackingField,
        data: ResolutionMode
    ): FirStatement {
        return declarationsTransformer.transformBackingField(backingField, data)
    }

    override fun transformField(field: FirField, data: ResolutionMode): FirField {
        return declarationsTransformer.transformField(field, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
        return declarationsTransformer.transformRegularClass(regularClass, data)
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: ResolutionMode
    ): FirStatement {
        return declarationsTransformer.transformAnonymousObject(anonymousObject, data)
    }

    override fun transformAnonymousObjectExpression(
        anonymousObjectExpression: FirAnonymousObjectExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformAnonymousObjectExpression(anonymousObjectExpression, data)
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction {
        return declarationsTransformer.transformSimpleFunction(simpleFunction, data)
    }

    override fun transformFunction(
        function: FirFunction,
        data: ResolutionMode
    ): FirStatement {
        return declarationsTransformer.transformFunction(function, data)
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        return declarationsTransformer.transformConstructor(constructor, data)
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): FirAnonymousInitializer {
        return declarationsTransformer.transformAnonymousInitializer(anonymousInitializer, data)
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ResolutionMode
    ): FirStatement {
        return declarationsTransformer.transformAnonymousFunction(anonymousFunction, data)
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: ResolutionMode
    ): FirStatement {
        return expressionsTransformer.transformAnonymousFunctionExpression(anonymousFunctionExpression, data)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): FirStatement {
        return declarationsTransformer.transformValueParameter(valueParameter, data)
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias {
        return declarationsTransformer.transformTypeAlias(typeAlias, data)
    }

    // ------------------------------------- Control flow statements -------------------------------------

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: ResolutionMode): FirStatement {
        return controlFlowStatementsTransformer.transformWhileLoop(whileLoop, data)
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: ResolutionMode): FirStatement {
        return controlFlowStatementsTransformer.transformDoWhileLoop(doWhileLoop, data)
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ResolutionMode): FirStatement {
        return controlFlowStatementsTransformer.transformWhenExpression(whenExpression, data)
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: ResolutionMode): FirWhenBranch {
        return controlFlowStatementsTransformer.transformWhenBranch(whenBranch, data)
    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: ResolutionMode
    ): FirStatement {
        return controlFlowStatementsTransformer.transformWhenSubjectExpression(whenSubjectExpression, data)
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: ResolutionMode): FirStatement {
        return controlFlowStatementsTransformer.transformTryExpression(tryExpression, data)
    }

    override fun transformCatch(catch: FirCatch, data: ResolutionMode): FirCatch {
        return controlFlowStatementsTransformer.transformCatch(catch, data)
    }

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: ResolutionMode): FirStatement {
        return controlFlowStatementsTransformer.transformJump(jump, data)
    }

    override fun transformReturnExpression(
        returnExpression: FirReturnExpression,
        data: ResolutionMode
    ): FirStatement {
        return controlFlowStatementsTransformer.transformReturnExpression(returnExpression, data)
    }

    override fun transformThrowExpression(
        throwExpression: FirThrowExpression,
        data: ResolutionMode
    ): FirStatement {
        return controlFlowStatementsTransformer.transformThrowExpression(throwExpression, data)
    }

    override fun transformElvisExpression(
        elvisExpression: FirElvisExpression,
        data: ResolutionMode
    ): FirStatement {
        return controlFlowStatementsTransformer.transformElvisExpression(elvisExpression, data)
    }

    // --------------------------------------------------------------------------

    fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform<FirElement, D>(transformer, data)
        require(result === this) { "become $result: `${result.render()}`, was ${this}: `${this.render()}`" }
    }
}
