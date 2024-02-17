/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.createCurrentScopeList
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.ScopeClassDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle

abstract class FirAbstractBodyResolveTransformerDispatcher(
    session: FirSession,
    phase: FirResolvePhase,
    override var implicitTypeOnly: Boolean,
    scopeSession: ScopeSession,
    val returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
    outerBodyResolveContext: BodyResolveContext? = null,
) : FirAbstractBodyResolveTransformer(phase) {

    open val preserveCFGForClasses: Boolean get() = !implicitTypeOnly
    open val buildCfgForScripts: Boolean get() = !implicitTypeOnly
    open val buildCfgForFiles: Boolean get() = !implicitTypeOnly

    final override val context: BodyResolveContext =
        outerBodyResolveContext ?: BodyResolveContext(returnTypeCalculator, DataFlowAnalyzerContext(session))
    final override val components: BodyResolveTransformerComponents =
        BodyResolveTransformerComponents(session, scopeSession, this, context)

    final override val resolutionContext: ResolutionContext = ResolutionContext(session, components, context)

    abstract val expressionsTransformer: FirExpressionsResolveTransformer?

    abstract val declarationsTransformer: FirDeclarationsResolveTransformer?

    private val controlFlowStatementsTransformer = FirControlFlowStatementsResolveTransformer(this)

    override fun transformFile(
        file: FirFile,
        data: ResolutionMode,
    ): FirFile = declarationTransformation(
        file,
        data,
        FirDeclarationsResolveTransformer::transformFile,
    )

    override fun transformScript(
        script: FirScript,
        data: ResolutionMode,
    ): FirScript = declarationTransformation(
        script,
        data,
        FirDeclarationsResolveTransformer::transformScript,
    )

    override fun transformCodeFragment(
        codeFragment: FirCodeFragment,
        data: ResolutionMode,
    ): FirCodeFragment = declarationTransformation(
        codeFragment,
        data,
        FirDeclarationsResolveTransformer::transformCodeFragment,
    )

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

        /**
         * We should transform a provided type to process such references in [transformAnnotationCall] by [transformForeignAnnotationCall]
         * because usually we do not run such transformations on replaced types explicitly
         */
        return data.expectedTypeRef.transformSingle(this, data)
    }

    // ------------------------------------- Expressions -------------------------------------

    private inline fun <T : R, R> expressionTransformation(
        expression: T,
        data: ResolutionMode,
        transformation: FirExpressionsResolveTransformer.(T, ResolutionMode) -> R,
    ): R {
        return expressionsTransformer?.transformation(expression, data) ?: expression
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): FirStatement {
        return expressionTransformation(expression, data, FirExpressionsResolveTransformer::transformExpression)
    }

    override fun transformWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: ResolutionMode,
    ): FirStatement {
        return transformElement(wrappedArgumentExpression, data)
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        qualifiedAccessExpression,
        data,
        FirExpressionsResolveTransformer::transformQualifiedAccessExpression,
    )

    override fun transformPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        propertyAccessExpression,
        data,
        FirExpressionsResolveTransformer::transformQualifiedAccessExpression,
    )

    override fun transformFunctionCall(
        functionCall: FirFunctionCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        functionCall,
        data,
        FirExpressionsResolveTransformer::transformFunctionCall,
    )

    override fun transformStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        stringConcatenationCall,
        data,
        FirExpressionsResolveTransformer::transformStringConcatenationCall,
    )

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        callableReferenceAccess,
        data,
        FirExpressionsResolveTransformer::transformCallableReferenceAccess,
    )

    override fun transformBlock(
        block: FirBlock,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        block,
        data,
        FirExpressionsResolveTransformer::transformBlock,
    )

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        thisReceiverExpression,
        data,
        FirExpressionsResolveTransformer::transformThisReceiverExpression,
    )

    override fun transformComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        comparisonExpression,
        data,
        FirExpressionsResolveTransformer::transformComparisonExpression,
    )

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        typeOperatorCall,
        data,
        FirExpressionsResolveTransformer::transformTypeOperatorCall,
    )

    override fun transformAssignmentOperatorStatement(
        assignmentOperatorStatement: FirAssignmentOperatorStatement,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        assignmentOperatorStatement,
        data,
        FirExpressionsResolveTransformer::transformAssignmentOperatorStatement,
    )

    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: FirIncrementDecrementExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        incrementDecrementExpression,
        data,
        FirExpressionsResolveTransformer::transformIncrementDecrementExpression,
    )

    override fun transformEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        equalityOperatorCall,
        data,
        FirExpressionsResolveTransformer::transformEqualityOperatorCall,
    )

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        checkNotNullCall,
        data,
        FirExpressionsResolveTransformer::transformCheckNotNullCall,
    )

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        binaryLogicExpression,
        data,
        FirExpressionsResolveTransformer::transformBinaryLogicExpression,
    )

    override fun transformDesugaredAssignmentValueReferenceExpression(
        desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        desugaredAssignmentValueReferenceExpression,
        data,
        FirExpressionsResolveTransformer::transformDesugaredAssignmentValueReferenceExpression,
    )

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        variableAssignment,
        data,
        FirExpressionsResolveTransformer::transformVariableAssignment,
    )

    override fun transformGetClassCall(
        getClassCall: FirGetClassCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        getClassCall,
        data,
        FirExpressionsResolveTransformer::transformGetClassCall,
    )

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode,
    ): FirStatement = declarationTransformation(
        wrappedDelegateExpression,
        data,
        FirDeclarationsResolveTransformer::transformWrappedDelegateExpression,
    )

    override fun <T> transformLiteralExpression(
        literalExpression: FirLiteralExpression<T>,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        literalExpression,
        data,
        FirExpressionsResolveTransformer::transformLiteralExpression,
    )

    override fun transformAnnotation(
        annotation: FirAnnotation,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        annotation,
        data,
        FirExpressionsResolveTransformer::transformAnnotation,
    )

    /**
     * @param symbol an owner of [annotationCall]
     * @param annotationCall an annotation call which does not belong to any declarations on the stack
     *
     * @see FirAnnotationCall.containingDeclarationSymbol
     */
    open fun transformForeignAnnotationCall(symbol: FirBasedSymbol<*>, annotationCall: FirAnnotationCall): FirAnnotationCall {
        return annotationCall
    }

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: ResolutionMode,
    ): FirStatement {
        val declarationSymbol = annotationCall.containingDeclarationSymbol
        if (declarationSymbol.fir !in context.containers.asReversed()) {
            return transformForeignAnnotationCall(declarationSymbol, annotationCall)
        }

        return expressionTransformation(
            annotationCall,
            data,
            FirExpressionsResolveTransformer::transformAnnotationCall,
        )
    }

    override fun transformErrorAnnotationCall(
        errorAnnotationCall: FirErrorAnnotationCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        errorAnnotationCall,
        data,
        FirExpressionsResolveTransformer::transformErrorAnnotationCall,
    )

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        delegatedConstructorCall,
        data,
        FirExpressionsResolveTransformer::transformDelegatedConstructorCall,
    )

    override fun transformAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        augmentedArraySetCall,
        data,
        FirExpressionsResolveTransformer::transformAugmentedArraySetCall,
    )

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        safeCallExpression,
        data,
        FirExpressionsResolveTransformer::transformSafeCallExpression,
    )

    override fun transformCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        checkedSafeCallSubject,
        data,
        FirExpressionsResolveTransformer::transformCheckedSafeCallSubject,
    )

    override fun transformArrayLiteral(
        arrayLiteral: FirArrayLiteral,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        arrayLiteral,
        data,
        FirExpressionsResolveTransformer::transformArrayLiteral,
    )

    override fun transformSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        smartCastExpression,
        data,
        FirExpressionsResolveTransformer::transformSmartCastExpression,
    )

    // ------------------------------------- Declarations -------------------------------------

    private inline fun <T : R, R : FirElement> declarationTransformation(
        declaration: T,
        data: ResolutionMode,
        transformation: FirDeclarationsResolveTransformer.(T, ResolutionMode) -> R,
    ): R {
        return declarationsTransformer?.transformation(declaration, data) ?: declaration
    }

    override fun transformDeclaration(
        declaration: FirDeclaration,
        data: ResolutionMode,
    ): FirDeclaration = declarationTransformation(
        declaration,
        data,
        FirDeclarationsResolveTransformer::transformDeclaration,
    )

    open fun transformDeclarationContent(
        declaration: FirDeclaration, data: ResolutionMode,
    ): FirDeclaration {
        return transformElement(declaration, data)
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: ResolutionMode,
    ): FirDeclarationStatus = declarationTransformation(
        declarationStatus,
        data,
        FirDeclarationsResolveTransformer::transformDeclarationStatus,
    )

    override fun transformEnumEntry(
        enumEntry: FirEnumEntry,
        data: ResolutionMode,
    ): FirEnumEntry = declarationTransformation(
        enumEntry,
        data,
        FirDeclarationsResolveTransformer::transformEnumEntry,
    )

    override fun transformDanglingModifierList(
        danglingModifierList: FirDanglingModifierList,
        data: ResolutionMode,
    ): FirDanglingModifierList = declarationTransformation(
        danglingModifierList,
        data,
        FirDeclarationsResolveTransformer::transformDanglingModifierList,
    )

    override fun transformFileAnnotationsContainer(
        fileAnnotationsContainer: FirFileAnnotationsContainer,
        data: ResolutionMode,
    ): FirFileAnnotationsContainer = declarationTransformation(
        fileAnnotationsContainer,
        data,
        FirDeclarationsResolveTransformer::transformFileAnnotationsContainer,
    )

    override fun transformProperty(
        property: FirProperty,
        data: ResolutionMode,
    ): FirProperty = declarationTransformation(
        property,
        data,
        FirDeclarationsResolveTransformer::transformProperty,
    )

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: ResolutionMode,
    ): FirPropertyAccessor = declarationTransformation(
        propertyAccessor,
        data,
        FirDeclarationsResolveTransformer::transformPropertyAccessor,
    )

    override fun transformBackingField(
        backingField: FirBackingField,
        data: ResolutionMode,
    ): FirBackingField = declarationTransformation(
        backingField,
        data,
        FirDeclarationsResolveTransformer::transformBackingField,
    )

    override fun transformField(
        field: FirField,
        data: ResolutionMode,
    ): FirField = declarationTransformation(
        field,
        data,
        FirDeclarationsResolveTransformer::transformField,
    )

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: ResolutionMode,
    ): FirRegularClass = declarationTransformation(
        regularClass,
        data,
        FirDeclarationsResolveTransformer::transformRegularClass,
    )

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: ResolutionMode,
    ): FirStatement = declarationTransformation(
        anonymousObject,
        data,
        FirDeclarationsResolveTransformer::transformAnonymousObject,
    )

    override fun transformAnonymousObjectExpression(
        anonymousObjectExpression: FirAnonymousObjectExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        anonymousObjectExpression,
        data,
        FirExpressionsResolveTransformer::transformAnonymousObjectExpression,
    )

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode,
    ): FirSimpleFunction = declarationTransformation(
        simpleFunction,
        data,
        FirDeclarationsResolveTransformer::transformSimpleFunction,
    )

    override fun transformFunction(
        function: FirFunction,
        data: ResolutionMode,
    ): FirFunction = declarationTransformation(
        function,
        data,
        FirDeclarationsResolveTransformer::transformFunction,
    )

    override fun transformConstructor(
        constructor: FirConstructor,
        data: ResolutionMode,
    ): FirConstructor = declarationTransformation(
        constructor,
        data,
        FirDeclarationsResolveTransformer::transformConstructor,
    )

    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: FirErrorPrimaryConstructor,
        data: ResolutionMode,
    ): FirErrorPrimaryConstructor = declarationTransformation(
        errorPrimaryConstructor,
        data,
        FirDeclarationsResolveTransformer::transformErrorPrimaryConstructor,
    )

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode,
    ): FirAnonymousInitializer = declarationTransformation(
        anonymousInitializer,
        data,
        FirDeclarationsResolveTransformer::transformAnonymousInitializer,
    )

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ResolutionMode,
    ): FirAnonymousFunction = declarationTransformation(
        anonymousFunction,
        data,
        FirDeclarationsResolveTransformer::transformAnonymousFunction,
    )

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: ResolutionMode,
    ): FirStatement = expressionTransformation(
        anonymousFunctionExpression,
        data,
        FirExpressionsResolveTransformer::transformAnonymousFunctionExpression,
    )

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: ResolutionMode,
    ): FirValueParameter = declarationTransformation(
        valueParameter,
        data,
        FirDeclarationsResolveTransformer::transformValueParameter,
    )

    override fun transformTypeAlias(
        typeAlias: FirTypeAlias,
        data: ResolutionMode,
    ): FirTypeAlias = declarationTransformation(
        typeAlias,
        data,
        FirDeclarationsResolveTransformer::transformTypeAlias,
    )

    // ------------------------------------- Control flow statements -------------------------------------

    private inline fun <T, R> controlFlowStatementsTransformation(
        declaration: T,
        data: ResolutionMode,
        transformation: FirControlFlowStatementsResolveTransformer.(T, ResolutionMode) -> R,
    ): R {
        return controlFlowStatementsTransformer.transformation(declaration, data)
    }

    override fun transformWhileLoop(
        whileLoop: FirWhileLoop,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        whileLoop,
        data,
        FirControlFlowStatementsResolveTransformer::transformWhileLoop,
    )

    override fun transformDoWhileLoop(
        doWhileLoop: FirDoWhileLoop,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        doWhileLoop,
        data,
        FirControlFlowStatementsResolveTransformer::transformDoWhileLoop,
    )

    override fun transformWhenExpression(
        whenExpression: FirWhenExpression,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        whenExpression,
        data,
        FirControlFlowStatementsResolveTransformer::transformWhenExpression,
    )

    override fun transformWhenBranch(
        whenBranch: FirWhenBranch,
        data: ResolutionMode,
    ): FirWhenBranch = controlFlowStatementsTransformation(
        whenBranch,
        data,
        FirControlFlowStatementsResolveTransformer::transformWhenBranch,
    )

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        whenSubjectExpression,
        data,
        FirControlFlowStatementsResolveTransformer::transformWhenSubjectExpression,
    )

    override fun transformTryExpression(
        tryExpression: FirTryExpression,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        tryExpression,
        data,
        FirControlFlowStatementsResolveTransformer::transformTryExpression,
    )

    override fun transformCatch(
        catch: FirCatch,
        data: ResolutionMode,
    ): FirCatch = controlFlowStatementsTransformation(
        catch,
        data,
        FirControlFlowStatementsResolveTransformer::transformCatch,
    )

    override fun <E : FirTargetElement> transformJump(
        jump: FirJump<E>,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        jump,
        data,
        FirControlFlowStatementsResolveTransformer::transformJump,
    )

    override fun transformReturnExpression(
        returnExpression: FirReturnExpression,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        returnExpression,
        data,
        FirControlFlowStatementsResolveTransformer::transformReturnExpression,
    )

    override fun transformThrowExpression(
        throwExpression: FirThrowExpression,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        throwExpression,
        data,
        FirControlFlowStatementsResolveTransformer::transformThrowExpression,
    )

    override fun transformElvisExpression(
        elvisExpression: FirElvisExpression,
        data: ResolutionMode,
    ): FirStatement = controlFlowStatementsTransformation(
        elvisExpression,
        data,
        FirControlFlowStatementsResolveTransformer::transformElvisExpression,
    )

    // --------------------------------------------------------------------------

    fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform<FirElement, D>(transformer, data)
        require(result === this) { "become $result: `${result.render()}`, was ${this}: `${this.render()}`" }
    }
}
