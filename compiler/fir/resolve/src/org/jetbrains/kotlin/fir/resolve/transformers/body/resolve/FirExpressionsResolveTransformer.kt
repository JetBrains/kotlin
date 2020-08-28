/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildVariableAssignment
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.FirStubInferenceSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StoreReceiver
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

open class FirExpressionsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes
    private val arrayOfCallTransformer = FirArrayOfCallTransformer()
    var enableArrayOfCallTransformation = false

    init {
        @Suppress("LeakingThis")
        components.callResolver.initTransformer(this)
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (expression.resultType is FirImplicitTypeRef && expression !is FirWrappedExpression) {
            val type = buildErrorTypeRef {
                source = expression.source
                diagnostic =
                    ConeSimpleDiagnostic("Type calculating for ${expression::class} is not supported", DiagnosticKind.InferenceError)
            }
            expression.resultType = type
        }
        return (expression.transformChildren(transformer, data) as FirStatement).compose()
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        qualifiedAccessExpression.transformAnnotations(this, data)
        qualifiedAccessExpression.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)

        var result = when (val callee = qualifiedAccessExpression.calleeReference) {
            // TODO: there was FirExplicitThisReference
            is FirThisReference -> {
                val labelName = callee.labelName
                val implicitReceiver = implicitReceiverStack[labelName]
                implicitReceiver?.boundSymbol?.let {
                    callee.replaceBoundSymbol(it)
                }
                val implicitType = implicitReceiver?.type
                qualifiedAccessExpression.resultType = when {
                    implicitReceiver is InaccessibleImplicitReceiverValue -> buildErrorTypeRef {
                        source = qualifiedAccessExpression.source
                        diagnostic = ConeInstanceAccessBeforeSuperCall("<this>")
                    }
                    implicitType != null -> buildResolvedTypeRef {
                        source = callee.source
                        type = implicitType
                    }
                    labelName != null -> buildErrorTypeRef {
                        source = qualifiedAccessExpression.source
                        diagnostic = ConeSimpleDiagnostic("Unresolved this@$labelName", DiagnosticKind.UnresolvedLabel)
                    }
                    else -> buildErrorTypeRef {
                        source = qualifiedAccessExpression.source
                        diagnostic = ConeSimpleDiagnostic("'this' is not defined in this context", DiagnosticKind.NoThis)
                    }
                }
                qualifiedAccessExpression
            }
            is FirSuperReference -> {
                transformSuperReceiver(callee, qualifiedAccessExpression, null)
            }
            is FirDelegateFieldReference -> {
                val delegateFieldSymbol = callee.resolvedSymbol
                qualifiedAccessExpression.resultType = delegateFieldSymbol.delegate.typeRef
                qualifiedAccessExpression
            }
            is FirResolvedNamedReference -> {
                if (qualifiedAccessExpression.typeRef !is FirResolvedTypeRef) {
                    storeTypeFromCallee(qualifiedAccessExpression)
                }
                qualifiedAccessExpression
            }
            else -> {
                val transformedCallee = callResolver.resolveVariableAccessAndSelectCandidate(qualifiedAccessExpression)
                // NB: here we can get raw expression because of dropped qualifiers (see transform callee),
                // so candidate existence must be checked before calling completion
                if (transformedCallee is FirQualifiedAccessExpression && transformedCallee.candidate() != null) {
                    if (!transformedCallee.isAcceptableResolvedQualifiedAccess()) {
                        return qualifiedAccessExpression.compose()
                    }
                    callCompleter.completeCall(transformedCallee, data.expectedType).result
                } else {
                    transformedCallee
                }
            }
        }
        when (result) {
            is FirQualifiedAccessExpression -> {
                dataFlowAnalyzer.enterQualifiedAccessExpression()
                result = components.transformQualifiedAccessUsingSmartcastInfo(result)
                dataFlowAnalyzer.exitQualifiedAccessExpression(result)
            }
            is FirResolvedQualifier -> {
                dataFlowAnalyzer.exitResolvedQualifierNode(result)
            }
        }
        return result.compose()
    }

    fun transformSuperReceiver(
        superReference: FirSuperReference,
        superReferenceContainer: FirQualifiedAccessExpression,
        containingCall: FirQualifiedAccess?
    ): FirQualifiedAccessExpression {
        val labelName = superReference.labelName
        val implicitReceiver =
            if (labelName != null) implicitReceiverStack[labelName] as? ImplicitDispatchReceiverValue
            else implicitReceiverStack.lastDispatchReceiver()
        implicitReceiver?.receiverExpression?.let {
            superReferenceContainer.transformDispatchReceiver(StoreReceiver, it)
        }
        when (val superTypeRef = superReference.superTypeRef) {
            is FirResolvedTypeRef -> {
                superReferenceContainer.resultType = superTypeRef
            }
            !is FirImplicitTypeRef -> {
                components.typeResolverTransformer.withAllowedBareTypes {
                    superReference.transformChildren(transformer, ResolutionMode.ContextIndependent)
                }

                val actualSuperType = (superReference.superTypeRef.coneType as? ConeClassLikeType)
                    ?.fullyExpandedType(session)?.let { superType ->
                        val classId = superType.lookupTag.classId
                        val superTypeRefs = implicitReceiver?.boundSymbol?.phasedFir?.superTypeRefs
                        val correspondingDeclaredSuperType = superTypeRefs?.firstOrNull {
                            it.coneType.fullyExpandedType(session).classId == classId
                        }?.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session) ?: return@let superType

                        if (superType.typeArguments.isEmpty() && correspondingDeclaredSuperType.typeArguments.isNotEmpty()) {
                            superType.withArguments(correspondingDeclaredSuperType.typeArguments)
                        } else {
                            superType
                        }
                    }
                /*
                 * See tests:
                 *   DiagnosticsTestGenerated$Tests$ThisAndSuper.testGenericQualifiedSuperOverridden
                 *   DiagnosticsTestGenerated$Tests$ThisAndSuper.testQualifiedSuperOverridden
                 */
                val actualSuperTypeRef = actualSuperType?.let {
                    buildResolvedTypeRef {
                        source = superTypeRef.source
                        type = it
                    }
                } ?: buildErrorTypeRef {
                    source = superTypeRef.source
                    diagnostic = ConeSimpleDiagnostic("Not a super type", DiagnosticKind.Other)
                }
                superReference.replaceSuperTypeRef(actualSuperTypeRef)

                superReferenceContainer.resultType = actualSuperTypeRef
            }
            else -> {
                val superTypeRefs = implicitReceiver?.boundSymbol?.phasedFir?.superTypeRefs
                val resultType = when {
                    superTypeRefs?.isNotEmpty() != true || containingCall == null -> {
                        buildErrorTypeRef {
                            source = superReferenceContainer.source
                            // NB: NOT_A_SUPERTYPE is reported by a separate checker
                            diagnostic = ConeStubDiagnostic(ConeSimpleDiagnostic("No super type", DiagnosticKind.Other))
                        }
                    }
                    else -> {
                        val types = components.findTypesForSuperCandidates(superTypeRefs, containingCall)
                        if (types.size == 1)
                            buildResolvedTypeRef {
                                source = superReferenceContainer.source?.fakeElement(FirFakeSourceElementKind.SuperCallImplicitType)
                                type = types.single()
                            }
                        else
                            buildErrorTypeRef {
                                source = superReferenceContainer.source
                                // NB: NOT_A_SUPERTYPE is reported by a separate checker
                                diagnostic = ConeStubDiagnostic(ConeSimpleDiagnostic("Ambiguous supertype", DiagnosticKind.Other))
                            }
                    }
                }
                superReferenceContainer.resultType = resultType
                superReference.replaceSuperTypeRef(resultType)
            }
        }
        return superReferenceContainer
    }

    protected open fun FirQualifiedAccessExpression.isAcceptableResolvedQualifiedAccess(): Boolean {
        return true
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        safeCallExpression.transformReceiver(this, ResolutionMode.ContextIndependent)

        val receiver = safeCallExpression.receiver

        dataFlowAnalyzer.enterSafeCallAfterNullCheck(safeCallExpression)

        safeCallExpression.apply {
            checkedSubjectRef.value.propagateTypeFromOriginalReceiver(receiver, components.session)
            transformRegularQualifiedAccess(this@FirExpressionsResolveTransformer, data)
            propagateTypeFromQualifiedAccessAfterNullCheck(receiver, session)
        }

        dataFlowAnalyzer.exitSafeCall(safeCallExpression)

        return safeCallExpression.compose()
    }

    override fun transformCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return checkedSafeCallSubject.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (functionCall.calleeReference is FirResolvedNamedReference && functionCall.resultType is FirImplicitTypeRef) {
            storeTypeFromCallee(functionCall)
        }
        if (functionCall.calleeReference !is FirSimpleNamedReference) return functionCall.compose()
        if (functionCall.calleeReference is FirNamedReferenceWithCandidate) return functionCall.compose()
        dataFlowAnalyzer.enterCall()
        functionCall.annotations.forEach { it.accept(this, data) }
        functionCall.transform<FirFunctionCall, Nothing?>(InvocationKindTransformer, null)
        functionCall.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)
        val expectedTypeRef = data.expectedType
        val (completeInference, callCompleted) =
            try {
                val initialExplicitReceiver = functionCall.explicitReceiver
                val resultExpression = callResolver.resolveCallAndSelectCandidate(functionCall)
                val resultExplicitReceiver = resultExpression.explicitReceiver
                if (initialExplicitReceiver !== resultExplicitReceiver && resultExplicitReceiver is FirQualifiedAccess) {
                    // name.invoke() case
                    callCompleter.completeCall(resultExplicitReceiver, noExpectedType)
                }
                callCompleter.completeCall(resultExpression, expectedTypeRef)
            } catch (e: Throwable) {
                throw RuntimeException("While resolving call ${functionCall.render()}", e)
            }

        dataFlowAnalyzer.exitFunctionCall(completeInference, callCompleted)
        if (callCompleted) {
            if (enableArrayOfCallTransformation) {
                arrayOfCallTransformer.toArrayOfCall(completeInference)?.let {
                    return it.compose()
                }
            }
        }
        return completeInference.compose()
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        withNewLocalScope {
            transformBlockInCurrentScope(block, data)
        }
        return block.compose()
    }

    internal fun transformBlockInCurrentScope(block: FirBlock, data: ResolutionMode) {
        dataFlowAnalyzer.enterBlock(block)
        val numberOfStatements = block.statements.size

        block.transformStatementsIndexed(transformer) { index ->
            val value = if (index == numberOfStatements - 1) data else ResolutionMode.ContextIndependent

            transformer.onBeforeStatementResolution(block.statements[index])

            TransformData.Data(value)
        }
        if (data == ResolutionMode.ContextIndependent) {
            block.transformStatements(integerLiteralTypeApproximator, null)
        } else {
            block.transformAllStatementsExceptLast(
                integerLiteralTypeApproximator,
                null
            )
        }
        block.transformOtherChildren(transformer, data)

        val resultExpression = when (val statement = block.statements.lastOrNull()) {
            is FirReturnExpression -> statement.result
            is FirExpression -> statement
            else -> null
        }
        block.resultType = if (resultExpression == null) {
            block.resultType.resolvedTypeFromPrototype(session.builtinTypes.unitType.type)
        } else {
            val theType = resultExpression.resultType
            if (theType is FirResolvedTypeRef) {
                buildResolvedTypeRef {
                    source = theType.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
                    type = theType.type
                    annotations += theType.annotations
                }
            } else {
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic("No type for block", DiagnosticKind.InferenceError)
                }
            }
        }

        dataFlowAnalyzer.exitBlock(block)
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        return transformQualifiedAccessExpression(thisReceiverExpression, data)
    }

    override fun transformComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return (comparisonExpression.transformChildren(transformer, ResolutionMode.ContextIndependent) as FirComparisonExpression).also {
            it.resultType = comparisonExpression.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)
        }.transformSingle(integerLiteralTypeApproximator, null).also(dataFlowAnalyzer::exitComparisonExpressionCall).compose()
    }

    override fun transformAssignmentOperatorStatement(
        assignmentOperatorStatement: FirAssignmentOperatorStatement,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        require(assignmentOperatorStatement.operation != FirOperation.ASSIGN)

        assignmentOperatorStatement.annotations.forEach { it.accept(this, data) }
        val leftArgument = assignmentOperatorStatement.leftArgument.transformSingle(transformer, ResolutionMode.ContextIndependent)
        val rightArgument = assignmentOperatorStatement.rightArgument.transformSingle(transformer, ResolutionMode.ContextDependent)

        fun createFunctionCall(name: Name) = buildFunctionCall {
            source = assignmentOperatorStatement.source?.fakeElement(FirFakeSourceElementKind.DesugaredCompoundAssignment)
            explicitReceiver = leftArgument
            argumentList = buildUnaryArgumentList(rightArgument)
            calleeReference = buildSimpleNamedReference {
                source = assignmentOperatorStatement.source
                this.name = name
                candidateSymbol = null
            }
        }

        // x.plusAssign(y)
        val assignmentOperatorName = FirOperationNameConventions.ASSIGNMENTS.getValue(assignmentOperatorStatement.operation)
        val assignOperatorCall = createFunctionCall(assignmentOperatorName)
        val resolvedAssignCall = resolveCandidateForAssignmentOperatorCall {
            assignOperatorCall.transformSingle(this, ResolutionMode.ContextDependent)
        }
        val assignCallReference = resolvedAssignCall.calleeReference as? FirNamedReferenceWithCandidate
        val assignIsError = assignCallReference?.isError ?: true
        // x = x + y
        val simpleOperatorName = FirOperationNameConventions.ASSIGNMENTS_TO_SIMPLE_OPERATOR.getValue(assignmentOperatorStatement.operation)
        val simpleOperatorCall = createFunctionCall(simpleOperatorName)
        val resolvedOperatorCall = resolveCandidateForAssignmentOperatorCall {
            simpleOperatorCall.transformSingle(this, ResolutionMode.ContextDependent)
        }
        val operatorCallReference = resolvedOperatorCall.calleeReference as? FirNamedReferenceWithCandidate
        val operatorIsError = operatorCallReference?.isError ?: true

        val lhsReference = leftArgument.toResolvedCallableReference()
        val lhsVariable = (lhsReference?.resolvedSymbol as? FirVariableSymbol<*>)?.fir
        val lhsIsVar = lhsVariable?.isVar == true
        return when {
            operatorIsError || (!lhsIsVar && !assignIsError) -> {
                callCompleter.completeCall(resolvedAssignCall, noExpectedType)
                dataFlowAnalyzer.exitFunctionCall(resolvedAssignCall, callCompleted = true)
                resolvedAssignCall.compose()
            }
            assignIsError -> {
                callCompleter.completeCall(resolvedOperatorCall, lhsVariable?.returnTypeRef ?: noExpectedType)
                dataFlowAnalyzer.exitFunctionCall(resolvedOperatorCall, callCompleted = true)
                val assignment =
                    buildVariableAssignment {
                        source = assignmentOperatorStatement.source
                        rValue = resolvedOperatorCall
                        calleeReference = when {
                            lhsIsVar -> lhsReference!!
                            else -> buildErrorNamedReference {
                                source = assignmentOperatorStatement.leftArgument.source
                                diagnostic = ConeVariableExpectedError()
                            }
                        }
                        (leftArgument as? FirQualifiedAccess)?.let {
                            dispatchReceiver = it.dispatchReceiver
                            extensionReceiver = it.extensionReceiver
                        }
                    }
                assignment.transform(transformer, ResolutionMode.ContextIndependent)
            }
            else -> {
                val operatorCallSymbol = operatorCallReference?.candidateSymbol
                val assignmentCallSymbol = assignCallReference?.candidateSymbol

                requireNotNull(operatorCallSymbol)
                requireNotNull(assignmentCallSymbol)

                buildErrorExpression {
                    source = assignmentOperatorStatement.source
                    diagnostic = ConeOperatorAmbiguityError(listOf(operatorCallSymbol, assignmentCallSymbol))
                }.compose()
            }
        }
    }

    override fun transformEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        // TODO: add approximation of integer literals
        val result = (equalityOperatorCall.transformChildren(transformer, ResolutionMode.ContextIndependent) as FirEqualityOperatorCall)
            .also { it.resultType = equalityOperatorCall.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type) }
            .transformSingle(integerLiteralTypeApproximator, null)
        dataFlowAnalyzer.exitEqualityOperatorCall(result)
        return result.compose()
    }

    private inline fun <T> resolveCandidateForAssignmentOperatorCall(block: () -> T): T {
        return dataFlowAnalyzer.withIgnoreFunctionCalls {
            callResolver.withNoArgumentsTransform {
                inferenceComponents.withInferenceSession(InferenceSessionForAssignmentOperatorCall) {
                    block()
                }
            }
        }
    }

    private object InferenceSessionForAssignmentOperatorCall : FirStubInferenceSession() {
        override fun <T> shouldRunCompletion(call: T): Boolean where T : FirStatement, T : FirResolvable = false
    }

    private fun FirTypeRef.withTypeArgumentsForBareType(argument: FirExpression): FirTypeRef {
        // TODO: Everything should also work for case of checked-type itself is a type alias
        val baseTypeArguments =
            argument.typeRef.coneTypeSafe<ConeKotlinType>()?.fullyExpandedType(session)?.typeArguments
        val type = coneTypeSafe<ConeKotlinType>()
        return if (type?.typeArguments?.isEmpty() != true ||
            type is ConeTypeParameterType ||
            baseTypeArguments?.isEmpty() != false ||
            (type is ConeClassLikeType &&
                    (type.lookupTag.toSymbol(session)?.fir as? FirTypeParameterRefsOwner)?.typeParameters?.isEmpty() == true)
        ) {
            this
        } else {
            withReplacedConeType(type.withArguments(baseTypeArguments))
        }
    }

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        val resolved = components.typeResolverTransformer.withAllowedBareTypes {
            typeOperatorCall.transformConversionTypeRef(transformer, ResolutionMode.ContextIndependent)
        }.transformOtherChildren(transformer, ResolutionMode.ContextIndependent)
        resolved.argumentList.transformArguments(integerLiteralTypeApproximator, null)
        val conversionTypeRef = resolved.conversionTypeRef.withTypeArgumentsForBareType(resolved.argument)
        resolved.transformChildren(object : FirDefaultTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
                return element.compose()
            }

            override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
                return if (typeRef === resolved.conversionTypeRef) {
                    conversionTypeRef.compose()
                } else {
                    typeRef.compose()
                }
            }
        }, null)
        when (resolved.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                resolved.resultType = session.builtinTypes.booleanType
            }
            FirOperation.AS -> {
                resolved.resultType = buildResolvedTypeRef {
                    source = conversionTypeRef.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
                    type = conversionTypeRef.coneType
                    annotations += conversionTypeRef.annotations
                }
            }
            FirOperation.SAFE_AS -> {
                resolved.resultType =
                    conversionTypeRef.withReplacedConeType(
                        conversionTypeRef.coneType.withNullability(
                            ConeNullability.NULLABLE, session.typeContext,
                        ),
                    )
            }
            else -> error("Unknown type operator")
        }
        dataFlowAnalyzer.exitTypeOperatorCall(resolved)
        return resolved.transform(integerLiteralTypeApproximator, null)
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        // Resolve the return type of a call to the synthetic function with signature:
        //   fun <K> checkNotNull(arg: K?): K
        // ...in order to get the not-nullable type of the argument.

        if (checkNotNullCall.calleeReference is FirResolvedNamedReference && checkNotNullCall.resultType !is FirImplicitTypeRef) {
            return checkNotNullCall.compose()
        }

        checkNotNullCall.argumentList.transformArguments(transformer, ResolutionMode.ContextDependent)

        var callCompleted = false
        val result = components.syntheticCallGenerator.generateCalleeForCheckNotNullCall(checkNotNullCall)?.let {
            val completionResult = callCompleter.completeCall(it, data.expectedType)
            callCompleted = completionResult.callCompleted
            completionResult.result
        } ?: run {
            checkNotNullCall.resultType =
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic("Can't resolve !! operator call", DiagnosticKind.InferenceError)
                }
            callCompleted = true
            checkNotNullCall
        }
        dataFlowAnalyzer.exitCheckNotNullCall(result, callCompleted)
        return result.compose()
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        val booleanType = binaryLogicExpression.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)
        return when (binaryLogicExpression.kind) {
            LogicOperationKind.AND ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryAnd)
                    .transformLeftOperand(this, ResolutionMode.WithExpectedType(booleanType))
                    .also(dataFlowAnalyzer::exitLeftBinaryAndArgument)
                    .transformRightOperand(this, ResolutionMode.WithExpectedType(booleanType)).also(dataFlowAnalyzer::exitBinaryAnd)

            LogicOperationKind.OR ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryOr)
                    .transformLeftOperand(this, ResolutionMode.WithExpectedType(booleanType))
                    .also(dataFlowAnalyzer::exitLeftBinaryOrArgument)
                    .transformRightOperand(this, ResolutionMode.WithExpectedType(booleanType)).also(dataFlowAnalyzer::exitBinaryOr)
        }.transformOtherChildren(transformer, ResolutionMode.WithExpectedType(booleanType)).also {
            it.resultType = booleanType
        }.compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        // val resolvedAssignment = transformCallee(variableAssignment)
        variableAssignment.annotations.forEach { it.accept(this, data) }
        val resolvedAssignment = callResolver.resolveVariableAccessAndSelectCandidate(variableAssignment)
        val result = if (resolvedAssignment is FirVariableAssignment) {
            val completeAssignment = callCompleter.completeCall(resolvedAssignment, noExpectedType).result // TODO: check
            val expectedType = components.typeFromCallee(completeAssignment)
            completeAssignment.transformRValue(transformer, withExpectedType(expectedType))
                .transformRValue(integerLiteralTypeApproximator, expectedType.coneTypeSafe())
        } else {
            // This can happen in erroneous code only
            resolvedAssignment
        }
        (result as? FirVariableAssignment)?.let { dataFlowAnalyzer.exitVariableAssignment(it) }
        return result.compose()
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        if (callableReferenceAccess.calleeReference is FirResolvedNamedReference) {
            return callableReferenceAccess.compose()
        }

        callableReferenceAccess.annotations.forEach { it.accept(this, data) }
        val explicitReceiver = callableReferenceAccess.explicitReceiver
        val transformedLHS = explicitReceiver?.transformSingle(this, ResolutionMode.ContextIndependent)?.apply {
            if (this is FirResolvedQualifier && callableReferenceAccess.hasQuestionMarkAtLHS) {
                replaceIsNullableLHSForCallableReference(true)
            }
        }

        val callableReferenceAccessWithTransformedLHS =
            if (transformedLHS != null)
                callableReferenceAccess.transformExplicitReceiver(StoreReceiver, transformedLHS)
            else
                callableReferenceAccess

        if (data !is ResolutionMode.ContextDependent) {
            val resolvedReference =
                components.syntheticCallGenerator.resolveCallableReferenceWithSyntheticOuterCall(
                    callableReferenceAccess, data.expectedType,
                ) ?: callableReferenceAccess

            return resolvedReference.compose()
        }

        return callableReferenceAccessWithTransformedLHS.compose()
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        val arg = getClassCall.argument
        val dataWithExpectedType = if (arg is FirConstExpression<*>) {
            withExpectedType(arg.typeRef.resolvedTypeFromPrototype(arg.kind.expectedConeType()))
        } else {
            data
        }
        val transformedGetClassCall = transformExpression(getClassCall, dataWithExpectedType).single as FirGetClassCall

        val typeOfExpression = when (val lhs = transformedGetClassCall.argument) {
            is FirResolvedQualifier -> {
                val symbol = lhs.symbol
                val typeArguments: Array<ConeTypeProjection> =
                    if (lhs.typeArguments.isNotEmpty()) {
                        // If type arguments exist, use them to construct the type of the expression.
                        lhs.typeArguments.map { it.toConeTypeProjection() }.toTypedArray()
                    } else {
                        // Otherwise, prepare the star projections as many as the size of type parameters.
                        Array((symbol?.phasedFir as? FirTypeParameterRefsOwner)?.typeParameters?.size ?: 0) {
                            ConeStarProjection
                        }
                    }
                val typeRef = symbol?.constructType(typeArguments, isNullable = false)
                if (typeRef != null) {
                    lhs.replaceTypeRef(buildResolvedTypeRef { type = typeRef })
                    typeRef
                } else {
                    lhs.resultType.coneType
                }
            }
            is FirResolvedReifiedParameterReference -> {
                val symbol = lhs.symbol
                symbol.constructType(emptyArray(), isNullable = false)
            }
            else -> {
                lhs.resultType.coneType
            }
        }

        transformedGetClassCall.resultType =
            buildResolvedTypeRef {
                type = StandardClassIds.KClass.constructClassLikeType(arrayOf(typeOfExpression), false)
            }
        return transformedGetClassCall.compose()
    }

    private fun FirConstKind<*>.expectedConeType(): ConeKotlinType {
        fun constructLiteralType(classId: ClassId, isNullable: Boolean = false): ConeKotlinType {
            val symbol = symbolProvider.getClassLikeSymbolByFqName(classId)
                ?: return ConeClassErrorType(ConeSimpleDiagnostic("Missing stdlib class: $classId", DiagnosticKind.MissingStdlibClass))
            return symbol.toLookupTag().constructClassType(emptyArray(), isNullable)
        }
        return when (this) {
            FirConstKind.Null -> session.builtinTypes.nullableNothingType.type
            FirConstKind.Boolean -> session.builtinTypes.booleanType.type
            FirConstKind.Char -> constructLiteralType(StandardClassIds.Char)
            FirConstKind.Byte -> constructLiteralType(StandardClassIds.Byte)
            FirConstKind.Short -> constructLiteralType(StandardClassIds.Short)
            FirConstKind.Int -> constructLiteralType(StandardClassIds.Int)
            FirConstKind.Long -> constructLiteralType(StandardClassIds.Long)
            FirConstKind.String -> constructLiteralType(StandardClassIds.String)
            FirConstKind.Float -> constructLiteralType(StandardClassIds.Float)
            FirConstKind.Double -> constructLiteralType(StandardClassIds.Double)

            FirConstKind.UnsignedByte -> constructLiteralType(StandardClassIds.UByte)
            FirConstKind.UnsignedShort -> constructLiteralType(StandardClassIds.UShort)
            FirConstKind.UnsignedInt -> constructLiteralType(StandardClassIds.UInt)
            FirConstKind.UnsignedLong -> constructLiteralType(StandardClassIds.ULong)

            FirConstKind.IntegerLiteral -> constructLiteralType(StandardClassIds.Int)
            FirConstKind.UnsignedIntegerLiteral -> constructLiteralType(StandardClassIds.UInt)
        }
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        constExpression.annotations.forEach { it.accept(this, data) }

        val type = when (val kind = constExpression.kind) {
            FirConstKind.IntegerLiteral, FirConstKind.UnsignedIntegerLiteral -> {
                val integerLiteralType =
                    ConeIntegerLiteralTypeImpl(constExpression.value as Long, isUnsigned = kind == FirConstKind.UnsignedIntegerLiteral)
                val expectedType = data.expectedType?.coneTypeSafe<ConeKotlinType>()
                if (expectedType != null) {
                    val approximatedType = integerLiteralType.getApproximatedType(expectedType)
                    val newConstKind = approximatedType.toConstKind()
                    if (newConstKind == null) {
                        @Suppress("UNCHECKED_CAST")
                        constExpression.replaceKind(FirConstKind.Int as FirConstKind<T>)
                        dataFlowAnalyzer.exitConstExpression(constExpression as FirConstExpression<*>)
                        constExpression.resultType = buildErrorTypeRef {
                            source = constExpression.source
                            diagnostic = ConeTypeMismatchError(expectedType, integerLiteralType.getApproximatedType())
                        }
                        return constExpression.compose()
                    }
                    @Suppress("UNCHECKED_CAST")
                    constExpression.replaceKind(newConstKind as FirConstKind<T>)
                    approximatedType
                } else {
                    integerLiteralType
                }
            }
            else -> kind.expectedConeType()
        }

        dataFlowAnalyzer.exitConstExpression(constExpression as FirConstExpression<*>)
        constExpression.resultType = constExpression.resultType.resolvedTypeFromPrototype(type)
        return constExpression.compose()
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (annotationCall.resolveStatus == FirAnnotationResolveStatus.Resolved) return annotationCall.compose()
        return resolveAnnotationCall(annotationCall, FirAnnotationResolveStatus.Resolved)
    }

    protected fun resolveAnnotationCall(
        annotationCall: FirAnnotationCall,
        status: FirAnnotationResolveStatus
    ): CompositeTransformResult<FirAnnotationCall> {
        dataFlowAnalyzer.enterAnnotationCall(annotationCall)
        return withFirArrayOfCallTransformer {
            annotationCall.transformAnnotationTypeRef(transformer, ResolutionMode.ContextIndependent)
            if (status == FirAnnotationResolveStatus.PartiallyResolved) return annotationCall.compose()
            val result = callResolver.resolveAnnotationCall(annotationCall) ?: return annotationCall.compose()
            callCompleter.completeCall(result, noExpectedType)
            result.replaceResolveStatus(status)
            dataFlowAnalyzer.exitAnnotationCall(result)
            annotationCall.compose()
        }
    }

    private inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            enableArrayOfCallTransformation = false
        }
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        if (transformer.implicitTypeOnly) return delegatedConstructorCall.compose()
        when (delegatedConstructorCall.calleeReference) {
            is FirResolvedNamedReference, is FirErrorNamedReference -> return delegatedConstructorCall.compose()
        }
        val containers = components.context.containers
        val containingClass = containers[containers.lastIndex - 1] as FirClass<*>
        val containingConstructor = containers.last() as FirConstructor
        if (delegatedConstructorCall.isSuper && delegatedConstructorCall.constructedTypeRef is FirImplicitTypeRef) {
            val superClass = containingClass.superTypeRefs.firstOrNull {
                if (it !is FirResolvedTypeRef) return@firstOrNull false
                val declaration = extractSuperTypeDeclaration(it) ?: return@firstOrNull false
                declaration.classKind == ClassKind.CLASS
            } as FirResolvedTypeRef? ?: session.builtinTypes.anyType
            delegatedConstructorCall.replaceConstructedTypeRef(superClass)
            delegatedConstructorCall.replaceCalleeReference(buildExplicitSuperReference {
                source = delegatedConstructorCall.calleeReference.source
                superTypeRef = superClass
            })
        }

        dataFlowAnalyzer.enterCall()
        var callCompleted = true
        var result = delegatedConstructorCall
        try {
            val lastDispatchReceiver = implicitReceiverStack.lastDispatchReceiver()
            context.withTowerDataCleanup {
                if ((context.containerIfAny as? FirConstructor)?.isPrimary == true) {
                    context.replaceTowerDataContext(context.getTowerDataContextForConstructorResolution())
                    context.getPrimaryConstructorParametersScope()?.let(context::addLocalScope)
                }

                // it's just a constructor parameters scope created in
                // `FirDeclarationResolveTransformer::doTransformConstructor()`
                val parametersScope = context.towerDataContext.localScopes.lastOrNull()

                // because there's a `context.saveContextForAnonymousFunction(anonymousFunction)`
                // call inside of the FirDeclarationResolveTransformer and accessing `this`
                // inside a lambda which is a value parameter of a constructor delegate
                // is prohibited
                context.withTowerDataContext(context.getTowerDataContextForConstructorResolution()) {
                    parametersScope?.let {
                        addLocalScope(it)
                    }
                    if (containingClass is FirRegularClass && !containingConstructor.isPrimary) {
                        context.addReceiver(
                            null,
                            InaccessibleImplicitReceiverValue(
                                containingClass.symbol,
                                containingClass.defaultType(),
                                session,
                                scopeSession
                            )
                        )
                    }
                    delegatedConstructorCall.transformChildren(transformer, ResolutionMode.ContextDependent)
                }
            }
            val reference = delegatedConstructorCall.calleeReference
            val constructorType: ConeClassLikeType = when (reference) {
                is FirThisReference -> {
                    lastDispatchReceiver?.type as? ConeClassLikeType ?: return delegatedConstructorCall.compose()
                }
                is FirSuperReference -> {
                    // TODO: unresolved supertype
                    val supertype = reference.superTypeRef.coneTypeSafe<ConeClassLikeType>()
                        ?.takeIf { it !is ConeClassErrorType } ?: return delegatedConstructorCall.compose()
                    supertype.fullyExpandedType(session)
                }
                else -> return delegatedConstructorCall.compose()
            }

            val resolvedCall =
                callResolver.resolveDelegatingConstructorCall(delegatedConstructorCall, constructorType)
                    ?: return delegatedConstructorCall.compose()
            if (reference is FirThisReference && reference.boundSymbol == null) {
                resolvedCall.dispatchReceiver.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.toSymbol(session)?.let {
                    reference.replaceBoundSymbol(it)
                }
            }

            // it seems that we may leave this code as is
            // without adding `context.withTowerDataContext(context.getTowerDataContextForConstructorResolution())`
            val completionResult = callCompleter.completeCall(resolvedCall, noExpectedType)
            result = completionResult.result
            callCompleted = completionResult.callCompleted
            return result.compose()
        } finally {
            dataFlowAnalyzer.exitDelegatedConstructorCall(result, callCompleted)
        }
    }

    private fun extractSuperTypeDeclaration(typeRef: FirTypeRef): FirRegularClass? {
        if (typeRef !is FirResolvedTypeRef) return null
        return when (val declaration = typeRef.firClassLike(session)) {
            is FirRegularClass -> declaration
            is FirTypeAlias -> extractSuperTypeDeclaration(declaration.expandedTypeRef)
            else -> null
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun transformAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        assert(augmentedArraySetCall.operation in FirOperation.ASSIGNMENTS)
        assert(augmentedArraySetCall.operation != FirOperation.ASSIGN)

        val operatorName = FirOperationNameConventions.ASSIGNMENTS.getValue(augmentedArraySetCall.operation)

        val firstCalls = with(augmentedArraySetCall.setGetBlock.statements.last() as FirFunctionCall) setCall@{
            buildList {
                add(this@setCall)
                with(arguments.last() as FirFunctionCall) plusCall@{
                    add(this@plusCall)
                    add(explicitReceiver as FirFunctionCall)
                }
            }
        }
        val secondCalls = listOf(
            augmentedArraySetCall.assignCall,
            augmentedArraySetCall.assignCall.explicitReceiver as FirFunctionCall
        )

        val firstResult = withLocalScopeCleanup {
            augmentedArraySetCall.setGetBlock.transformSingle(transformer, ResolutionMode.ContextIndependent)
        }
        val secondResult = augmentedArraySetCall.assignCall.transformSingle(transformer, ResolutionMode.ContextIndependent)

        val firstSucceed = firstCalls.all { it.typeRef !is FirErrorTypeRef }
        val secondSucceed = secondCalls.all { it.typeRef !is FirErrorTypeRef }

        val result: FirStatement = when {
            firstSucceed && secondSucceed -> {
                augmentedArraySetCall.also {
                    it.replaceCalleeReference(
                        buildErrorNamedReference {
                            // TODO: add better diagnostic
                            source = augmentedArraySetCall.source
                            diagnostic = ConeAmbiguityError(operatorName, CandidateApplicability.RESOLVED, emptyList())
                        }
                    )
                }
            }
            firstSucceed -> firstResult
            secondSucceed -> secondResult
            else -> {
                augmentedArraySetCall.also {
                    it.replaceCalleeReference(
                        buildErrorNamedReference {
                            source = augmentedArraySetCall.source
                            diagnostic = ConeUnresolvedNameError(operatorName)
                        }
                    )
                }
            }
        }
        return result.compose()
    }

    // ------------------------------------------------------------------------------------------------

    internal fun <T> storeTypeFromCallee(access: T) where T : FirQualifiedAccess, T : FirExpression {
        access.resultType = callCompleter.typeFromCallee(access)
    }
}
