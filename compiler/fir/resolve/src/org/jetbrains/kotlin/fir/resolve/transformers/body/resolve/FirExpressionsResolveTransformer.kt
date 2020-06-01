/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import com.intellij.openapi.progress.ProcessCanceledException
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
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StoreReceiver
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class FirExpressionsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes
    private val arrayOfCallTransformer = FirArrayOfCallTransformer()
    var enableArrayOfCallTransformation = false

    init {
        components.callResolver.initTransformer(this)
    }

    private inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            enableArrayOfCallTransformation = false
        }
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
        qualifiedAccessExpression.annotations.forEach { it.accept(this, data) }
        qualifiedAccessExpression.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)

        var result = when (val callee = qualifiedAccessExpression.calleeReference) {
            // TODO: there was FirExplicitThisReference
            is FirThisReference -> {
                val labelName = callee.labelName
                val implicitReceiver = implicitReceiverStack[labelName]
                implicitReceiver?.boundSymbol?.let {
                    callee.replaceBoundSymbol(it)
                }
                qualifiedAccessExpression.resultType = buildResolvedTypeRef {
                    type = implicitReceiver?.type ?: ConeKotlinErrorType("Unresolved this@$labelName")
                }
                qualifiedAccessExpression
            }
            is FirSuperReference -> {
                val labelName = callee.labelName
                val implicitReceiver =
                    if (labelName != null) implicitReceiverStack[labelName] as? ImplicitDispatchReceiverValue
                    else implicitReceiverStack.lastDispatchReceiver()
                implicitReceiver?.receiverExpression?.let {
                    qualifiedAccessExpression.transformDispatchReceiver(StoreReceiver, it)
                }
                when (val superTypeRef = callee.superTypeRef) {
                    is FirResolvedTypeRef -> {
                        qualifiedAccessExpression.resultType = superTypeRef
                    }
                    !is FirImplicitTypeRef -> {
                        callee.transformChildren(transformer, ResolutionMode.ContextIndependent)
                        qualifiedAccessExpression.resultType = callee.superTypeRef
                    }
                    else -> {
                        val superTypeRefs = implicitReceiver?.boundSymbol?.phasedFir?.superTypeRefs
                        val resultType = when {
                            superTypeRefs?.isNotEmpty() != true -> {
                                buildErrorTypeRef {
                                    source = qualifiedAccessExpression.source
                                    // NB: NOT_A_SUPERTYPE is reported by a separate checker
                                    diagnostic = ConeStubDiagnostic(ConeSimpleDiagnostic("No super type", DiagnosticKind.Other))
                                }
                            }
                            superTypeRefs.size == 1 -> {
                                superTypeRefs.single()
                            }
                            else -> {
                                buildComposedSuperTypeRef {
                                    source = qualifiedAccessExpression.source
                                    superTypeRefs.mapTo(this.superTypeRefs) { it as FirResolvedTypeRef }
                                }
                            }
                        }
                        qualifiedAccessExpression.resultType = resultType
                        callee.replaceSuperTypeRef(resultType)
                    }
                }
                qualifiedAccessExpression
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
                    callCompleter.completeCall(transformedCallee, data.expectedType).result
                } else {
                    transformedCallee
                }
            }
        }
        when (result) {
            is FirQualifiedAccessExpression -> {
                dataFlowAnalyzer.enterQualifiedAccessExpression(result)
                result = components.transformQualifiedAccessUsingSmartcastInfo(result)
                dataFlowAnalyzer.exitQualifiedAccessExpression(result)
            }
            is FirResolvedQualifier -> {
                dataFlowAnalyzer.exitResolvedQualifierNode(result)
            }
        }
        return result.compose()
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        safeCallExpression.transformReceiver(this, ResolutionMode.ContextIndependent)

        val receiver = safeCallExpression.receiver

        dataFlowAnalyzer.enterSafeCallAfterNullCheck(safeCallExpression)

        safeCallExpression.apply {
            checkedSubject.value.propagateTypeFromOriginalReceiver(receiver, components.session)
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
        dataFlowAnalyzer.enterCall(functionCall)
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
                val completionResult = callCompleter.completeCall(resultExpression, expectedTypeRef)

                if (completionResult.result.typeRef is FirErrorTypeRef) {
                    completionResult.result.argumentList.transformArguments(transformer, ResolutionMode.LambdaResolution(null))
                }
                completionResult
            } catch (e: ProcessCanceledException) {
                throw e
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
            (resultExpression.resultType as? FirResolvedTypeRef) ?: buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic("No type for block", DiagnosticKind.InferenceError)
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

    override fun transformOperatorCall(operatorCall: FirOperatorCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (operatorCall.operation in FirOperation.BOOLEANS) {
            // TODO: add approximation of integer literals
            val result = (operatorCall.transformChildren(transformer, ResolutionMode.ContextIndependent) as FirOperatorCall).also {
                it.resultType = operatorCall.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)
            }.transformSingle(integerLiteralTypeApproximator, null)
            dataFlowAnalyzer.exitOperatorCall(result)
            return result.compose()
        }

        if (operatorCall.operation in FirOperation.ASSIGNMENTS) {
            require(operatorCall.operation != FirOperation.ASSIGN)

            operatorCall.annotations.forEach { it.accept(this, data) }
            @Suppress("NAME_SHADOWING")
            operatorCall.argumentList.transformArguments(this, ResolutionMode.ContextIndependent)
            val (leftArgument, rightArgument) = operatorCall.arguments

            fun createFunctionCall(name: Name) = buildFunctionCall {
                source = operatorCall.source
                explicitReceiver = leftArgument
                argumentList = buildUnaryArgumentList(rightArgument)
                calleeReference = buildSimpleNamedReference {
                    source = operatorCall.source
                    this.name = name
                    candidateSymbol = null
                }
            }

            // TODO: disable DataFlowAnalyzer for resolving that two calls
            // x.plusAssign(y)
            val assignmentOperatorName = FirOperationNameConventions.ASSIGNMENTS.getValue(operatorCall.operation)
            val assignOperatorCall = createFunctionCall(assignmentOperatorName)
            val resolvedAssignCall = assignOperatorCall.transformSingle(this, ResolutionMode.ContextIndependent)
            val assignCallReference = resolvedAssignCall.toResolvedCallableReference()
            val assignIsError = resolvedAssignCall.typeRef is FirErrorTypeRef
            // x = x + y
            val simpleOperatorName = FirOperationNameConventions.ASSIGNMENTS_TO_SIMPLE_OPERATOR.getValue(operatorCall.operation)
            val simpleOperatorCall = createFunctionCall(simpleOperatorName)
            val resolvedOperatorCall = simpleOperatorCall.transformSingle(this, ResolutionMode.ContextIndependent)
            val operatorCallReference = resolvedOperatorCall.toResolvedCallableReference()

            val lhsReference = leftArgument.toResolvedCallableReference()
            val lhsIsVar = (lhsReference?.resolvedSymbol as? FirVariableSymbol<*>)?.fir?.isVar == true
            return when {
                operatorCallReference == null || (!lhsIsVar && !assignIsError) -> resolvedAssignCall.compose()
                assignCallReference == null -> {
                    val assignment =
                        buildVariableAssignment {
                            source = operatorCall.source
                            safe = false
                            rValue = resolvedOperatorCall
                            calleeReference = if (lhsIsVar)
                                lhsReference!!
                            else
                                buildErrorNamedReference {
                                    source = operatorCall.argument.source
                                    diagnostic = ConeVariableExpectedError()
                                }
                            (leftArgument as? FirQualifiedAccess)?.let {
                                dispatchReceiver = it.dispatchReceiver
                                extensionReceiver = it.extensionReceiver
                            }
                        }
                    assignment.transform(transformer, ResolutionMode.ContextIndependent)
                }
                else -> buildErrorExpression {
                    source = operatorCall.source
                    diagnostic =
                        ConeOperatorAmbiguityError(listOf(operatorCallReference.resolvedSymbol, assignCallReference.resolvedSymbol))
                }.compose()
            }
        }

        throw IllegalArgumentException(operatorCall.render())
    }

    private fun FirTypeRef.withTypeArgumentsForBareType(argument: FirExpression): FirTypeRef {
        val baseTypeArguments = argument.typeRef.coneTypeSafe<ConeKotlinType>()?.typeArguments
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
        val resolved = transformExpression(typeOperatorCall, data).single as FirTypeOperatorCall
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
                resolved.resultType = conversionTypeRef
            }
            FirOperation.SAFE_AS -> {
                resolved.resultType =
                    conversionTypeRef.withReplacedConeType(
                        conversionTypeRef.coneTypeUnsafe<ConeKotlinType>().withNullability(
                            ConeNullability.NULLABLE, session.inferenceContext,
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

        var callCompleted: Boolean = false
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
        // TODO: maybe replace with FirAbstractAssignment for performance?
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
            if (this is FirResolvedQualifier && callableReferenceAccess.safe) {
                replaceSafe(true)
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
        val transformedGetClassCall = transformExpression(getClassCall, data).single as FirGetClassCall

        val typeOfExpression = when (val lhs = transformedGetClassCall.argument) {
            is FirResolvedQualifier -> {
                val symbol = lhs.symbol
                val typeRef =
                    symbol?.constructType(
                        Array((symbol.phasedFir as? FirTypeParameterRefsOwner)?.typeParameters?.size ?: 0) {
                            ConeStarProjection
                        },
                        isNullable = false,
                    )
                if (typeRef != null) {
                    lhs.replaceTypeRef(buildResolvedTypeRef { type = typeRef })
                    typeRef
                } else {
                    lhs.resultType.coneTypeUnsafe()
                }
            }
            is FirResolvedReifiedParameterReference -> {
                val symbol = lhs.symbol
                symbol.constructType(emptyArray(), isNullable = false)
            }
            else -> {
                lhs.resultType.coneTypeUnsafe<ConeKotlinType>()
            }
        }

        transformedGetClassCall.resultType =
            buildResolvedTypeRef {
                type = StandardClassIds.KClass.constructClassLikeType(arrayOf(typeOfExpression), false)
            }
        return transformedGetClassCall.compose()
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        constExpression.annotations.forEach { it.accept(this, data) }
        fun constructLiteralType(classId: ClassId, isNullable: Boolean = false): ConeKotlinType {
            val symbol = symbolProvider.getClassLikeSymbolByFqName(classId) ?: return ConeClassErrorType("Missing stdlib class: ${classId}")
            return symbol.toLookupTag().constructClassType(emptyArray(), isNullable)
        }

        val kind = constExpression.kind
        val type = when (kind) {
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
            FirConstKind.IntegerLiteral, FirConstKind.UnsignedIntegerLiteral -> {
                val integerLiteralType =
                    ConeIntegerLiteralTypeImpl(constExpression.value as Long, isUnsigned = kind == FirConstKind.UnsignedIntegerLiteral)
                val expectedType = data.expectedType?.coneTypeSafe<ConeKotlinType>()
                if (expectedType != null) {
                    val approximatedType = integerLiteralType.getApproximatedType(expectedType)
                    val newConstKind = approximatedType.toConstKind()
                    if (newConstKind == null) {
                        constExpression.replaceKind(FirConstKind.Int as FirConstKind<T>)
                        dataFlowAnalyzer.exitConstExpresion(constExpression as FirConstExpression<*>)
                        constExpression.resultType = buildErrorTypeRef {
                            source = constExpression.source
                            diagnostic = ConeTypeMismatchError(expectedType, integerLiteralType.getApproximatedType())
                        }
                        return constExpression.compose()
                    }
                    constExpression.replaceKind(newConstKind as FirConstKind<T>)
                    approximatedType
                } else {
                    integerLiteralType
                }
            }

            FirConstKind.UnsignedByte -> constructLiteralType(StandardClassIds.UByte)
            FirConstKind.UnsignedShort -> constructLiteralType(StandardClassIds.UShort)
            FirConstKind.UnsignedInt -> constructLiteralType(StandardClassIds.UInt)
            FirConstKind.UnsignedLong -> constructLiteralType(StandardClassIds.ULong)
        }

        dataFlowAnalyzer.exitConstExpresion(constExpression as FirConstExpression<*>)
        constExpression.resultType = constExpression.resultType.resolvedTypeFromPrototype(type)
        return constExpression.compose()
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (annotationCall.resolved) return annotationCall.compose()
        dataFlowAnalyzer.enterAnnotationCall(annotationCall)
        return withFirArrayOfCallTransformer {
            (annotationCall.transformChildren(transformer, data) as FirAnnotationCall).also {
                // TODO: it's temporary incorrect solution until we design resolve and completion for annotation calls
                it.argumentList.transformArguments(integerLiteralTypeApproximator, null)
                it.replaceResolved(true)
                dataFlowAnalyzer.exitAnnotationCall(it)
            }.compose()
        }
    }

    private fun ConeTypeProjection.toFirTypeProjection(): FirTypeProjection = when (this) {
        is ConeStarProjection -> buildStarProjection()
        else -> {
            val type = when (this) {
                is ConeKotlinTypeProjectionIn -> type
                is ConeKotlinTypeProjectionOut -> type
                is ConeStarProjection -> throw IllegalStateException()
                else -> this as ConeKotlinType
            }
            buildTypeProjectionWithVariance {
                typeRef = buildResolvedTypeRef { this.type = type }
                variance = when (kind) {
                    ProjectionKind.IN -> Variance.IN_VARIANCE
                    ProjectionKind.OUT -> Variance.OUT_VARIANCE
                    ProjectionKind.INVARIANT -> Variance.INVARIANT
                    ProjectionKind.STAR -> throw IllegalStateException()
                }
            }
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
        if (delegatedConstructorCall.isSuper && delegatedConstructorCall.constructedTypeRef is FirImplicitTypeRef) {
            val containers = components.context.containers
            val containingClass = containers[containers.lastIndex - 1] as FirClass<*>
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

        dataFlowAnalyzer.enterCall(delegatedConstructorCall)
        var callCompleted = true
        var result = delegatedConstructorCall
        try {
            val lastDispatchReceiver = implicitReceiverStack.lastDispatchReceiver()
            context.withTowerDataCleanup {
                if ((context.containerIfAny as? FirConstructor)?.isPrimary == true) {
                    context.replaceTowerDataContext(context.getTowerDataContextForConstructorResolution())
                    context.getPrimaryConstructorParametersScope()?.let(context::addLocalScope)
                }

                delegatedConstructorCall.transformChildren(transformer, ResolutionMode.ContextDependent)
            }
            val typeArguments: List<FirTypeProjection>
            val reference = delegatedConstructorCall.calleeReference
            val symbol: FirClassSymbol<*> = when (reference) {
                is FirThisReference -> {
                    typeArguments = emptyList()
                    if (reference.boundSymbol == null) {
                        lastDispatchReceiver?.boundSymbol?.also {
                            reference.replaceBoundSymbol(it)
                        } ?: return delegatedConstructorCall.compose()
                    } else {
                        reference.boundSymbol!! as FirClassSymbol<*>
                    }
                }
                is FirSuperReference -> {
                    // TODO: unresolved supertype
                    val supertype = reference.superTypeRef.coneTypeSafe<ConeClassLikeType>() ?: return delegatedConstructorCall.compose()
                    val expandedSupertype = supertype.fullyExpandedType(session)
                    val symbol =
                        expandedSupertype.lookupTag.toSymbol(session) as? FirClassSymbol<*> ?: return delegatedConstructorCall.compose()
                    val classTypeParametersCount =
                        (symbol.fir as? FirTypeParameterRefsOwner)?.typeParameters?.count { it is FirTypeParameter } ?: 0
                    typeArguments = expandedSupertype.typeArguments
                        .takeLast(classTypeParametersCount) // Hack for KT-37525
                        .takeIf { it.isNotEmpty() }
                        ?.map { it.toFirTypeProjection() }
                        ?: emptyList()
                    symbol
                }
                else -> return delegatedConstructorCall.compose()
            }
            val resolvedCall = callResolver.resolveDelegatingConstructorCall(delegatedConstructorCall, symbol, typeArguments)
                ?: return delegatedConstructorCall.compose()
            if (reference is FirThisReference && reference.boundSymbol == null) {
                resolvedCall.dispatchReceiver.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.toSymbol(session)?.let {
                    reference.replaceBoundSymbol(it)
                }
            }

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
                            diagnostic = ConeAmbiguityError(operatorName, emptyList())
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
