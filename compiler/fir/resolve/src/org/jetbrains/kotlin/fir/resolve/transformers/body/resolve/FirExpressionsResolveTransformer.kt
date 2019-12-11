/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirErrorExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirVariableAssignmentImpl
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.impl.FirErrorNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirOperatorAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirTypeMismatchError
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirVariableExpectedError
import org.jetbrains.kotlin.fir.resolve.transformers.IntegerLiteralTypeApproximationTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StoreReceiver
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class FirExpressionsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private val callResolver: FirCallResolver get() = components.callResolver
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes

    init {
        components.callResolver.initTransformer(this)
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (expression.resultType is FirImplicitTypeRef && expression !is FirWrappedExpression) {
            val type = FirErrorTypeRefImpl(
                expression.source,
                FirSimpleDiagnostic("Type calculating for ${expression::class} is not supported", DiagnosticKind.InferenceError)
            )
            expression.resultType = type
        }
        return (expression.transformChildren(transformer, data) as FirStatement).compose()
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        qualifiedAccessExpression.annotations.forEach { it.accept(this, data) }
        var result = when (val callee = qualifiedAccessExpression.calleeReference) {
            is FirExplicitThisReference -> {
                val labelName = callee.labelName
                val implicitReceiver = implicitReceiverStack[labelName]
                implicitReceiver?.boundSymbol?.let {
                    callee.replaceBoundSymbol(it)
                }
                qualifiedAccessExpression.resultType = FirResolvedTypeRefImpl(
                    null, implicitReceiver?.type ?: ConeKotlinErrorType("Unresolved this@$labelName")
                )
                qualifiedAccessExpression
            }
            is FirSuperReference -> {
                when (val superTypeRef = callee.superTypeRef) {
                    is FirResolvedTypeRef -> {
                        qualifiedAccessExpression.resultType = superTypeRef
                    }
                    !is FirImplicitTypeRef -> {
                        callee.transformChildren(transformer, ResolutionMode.ContextIndependent)
                        qualifiedAccessExpression.resultType = callee.superTypeRef
                    }
                    else -> {
                        val superTypeRefFromStack = implicitReceiverStack.lastDispatchReceiver()
                            ?.boundSymbol?.phasedFir?.superTypeRefs?.firstOrNull()
                            ?: FirErrorTypeRefImpl(
                                qualifiedAccessExpression.source, FirSimpleDiagnostic("No super type", DiagnosticKind.NoSupertype)
                            )
                        qualifiedAccessExpression.resultType = superTypeRefFromStack
                        callee.replaceSuperTypeRef(superTypeRefFromStack)
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
                val transformedCallee = callResolver.resolveVariableAccessAndSelectCandidate(qualifiedAccessExpression, file)
                // NB: here we can get raw expression because of dropped qualifiers (see transform callee),
                // so candidate existence must be checked before calling completion
                if (transformedCallee is FirQualifiedAccessExpression && transformedCallee.candidate() != null) {
                    callCompleter.completeCall(transformedCallee, data.expectedType)
                } else {
                    transformedCallee
                }
            }
        }
        if (result is FirQualifiedAccessExpression) {
            dataFlowAnalyzer.enterQualifiedAccessExpression(result)
            result = components.transformQualifiedAccessUsingSmartcastInfo(result)
            dataFlowAnalyzer.exitQualifiedAccessExpression(result)
        }
        return result.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (functionCall.calleeReference is FirResolvedNamedReference && functionCall.resultType is FirImplicitTypeRef) {
            storeTypeFromCallee(functionCall)
        }
        if (functionCall.calleeReference !is FirSimpleNamedReference) return functionCall.compose()
        functionCall.annotations.forEach { it.accept(this, data) }
        functionCall.transform<FirFunctionCall, Nothing?>(InvocationKindTransformer, null)
        functionCall.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)
        val expectedTypeRef = data.expectedType
        val completeInference =
            try {
                val initialExplicitReceiver = functionCall.explicitReceiver
                val resultExpression = callResolver.resolveCallAndSelectCandidate(functionCall, file)
                val resultExplicitReceiver = resultExpression.explicitReceiver
                if (initialExplicitReceiver !== resultExplicitReceiver && resultExplicitReceiver is FirQualifiedAccess) {
                    // name.invoke() case
                    callCompleter.completeCall(resultExplicitReceiver, noExpectedType)
                }
                val completionResult = callCompleter.completeCall(resultExpression, expectedTypeRef)
                if (completionResult.typeRef is FirErrorTypeRef) {
                    completionResult.transformArguments(transformer, ResolutionMode.LambdaResolution(null))
                }
                completionResult
            } catch (e: Throwable) {
                throw RuntimeException("While resolving call ${functionCall.render()}", e)
            }

        dataFlowAnalyzer.exitFunctionCall(completeInference)
        return completeInference.compose()
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterBlock(block)
        @Suppress("NAME_SHADOWING")
        val block = block.transformChildren(transformer, data) as FirBlock
        val statement = block.statements.lastOrNull()

        val resultExpression = when (statement) {
            is FirReturnExpression -> statement.result
            is FirExpression -> statement
            else -> null
        }
        block.resultType = if (resultExpression == null) {
            block.resultType.resolvedTypeFromPrototype(session.builtinTypes.unitType.type)
        } else {
            (resultExpression.resultType as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(null, FirSimpleDiagnostic("No type for block", DiagnosticKind.InferenceError))
        }
        dataFlowAnalyzer.exitBlock(block)
        return block.compose()
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return transformQualifiedAccessExpression(thisReceiverExpression, data)
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
            val operatorCall = operatorCall.transformArguments(this, ResolutionMode.ContextIndependent)
            val (leftArgument, rightArgument) = operatorCall.arguments

            fun createFunctionCall(name: Name) = FirFunctionCallImpl(operatorCall.source).apply {
                explicitReceiver = leftArgument
                arguments += rightArgument
                calleeReference = FirSimpleNamedReference(
                    operatorCall.source,
                    name,
                    candidateSymbol = null
                )
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
                        FirVariableAssignmentImpl(operatorCall.source, false, resolvedOperatorCall).apply {
                            lValue = if (lhsIsVar)
                                lhsReference!!
                            else
                                FirErrorNamedReferenceImpl(operatorCall.arguments.first().source, FirVariableExpectedError())
                        }
                    assignment.transform(transformer, ResolutionMode.ContextIndependent)
                }
                else -> FirErrorExpressionImpl(
                    operatorCall.source,
                    FirOperatorAmbiguityError(listOf(operatorCallReference.resolvedSymbol, assignCallReference.resolvedSymbol))
                ).compose()
            }
        }

        throw IllegalArgumentException(operatorCall.render())
    }

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        val symbolProvider = session.firSymbolProvider
        val resolved = (transformExpression(typeOperatorCall, data).single as FirTypeOperatorCall)
            .transformArguments(integerLiteralTypeApproximator, null)
        when (resolved.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                resolved.resultType = FirResolvedTypeRefImpl(
                    null,
                    StandardClassIds.Boolean(symbolProvider).constructType(emptyArray(), isNullable = false)
                )
            }
            FirOperation.AS -> {
                resolved.resultType = resolved.conversionTypeRef
            }
            FirOperation.SAFE_AS -> {
                resolved.resultType =
                    resolved.conversionTypeRef.withReplacedConeType(
                        resolved.conversionTypeRef.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE)
                    )
            }
            else -> error("Unknown type operator")
        }
        dataFlowAnalyzer.exitTypeOperatorCall(typeOperatorCall)
        return resolved.transform(integerLiteralTypeApproximator, null)
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        // Resolve the return type of a call to the synthetic function with signature:
        //   fun <K> checkNotNull(arg: K?): K
        // ...in order to get the not-nullable type of the argument.

        if (checkNotNullCall.calleeReference is FirResolvedNamedReference && checkNotNullCall.resultType !is FirImplicitTypeRef) {
            return checkNotNullCall.compose()
        }

        checkNotNullCall.transformArguments(transformer, ResolutionMode.ContextDependent)

        val result = components.syntheticCallGenerator.generateCalleeForCheckNotNullCall(checkNotNullCall)?.let {
            callCompleter.completeCall(it, data.expectedType)
        } ?: run {
            checkNotNullCall.resultType =
                FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Can't resolve !! operator call", DiagnosticKind.InferenceError))
            checkNotNullCall
        }
        dataFlowAnalyzer.exitCheckNotNullCall(result)
        return result.compose()
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        val booleanType = binaryLogicExpression.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)
        return when (binaryLogicExpression.kind) {
            LogicOperationKind.AND ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryAnd)
                    .transformLeftOperand(this, ResolutionMode.WithExpectedType(booleanType)).also(dataFlowAnalyzer::exitLeftBinaryAndArgument)
                    .transformRightOperand(this, ResolutionMode.WithExpectedType(booleanType)).also(dataFlowAnalyzer::exitBinaryAnd)

            LogicOperationKind.OR ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryOr)
                    .transformLeftOperand(this, ResolutionMode.WithExpectedType(booleanType)).also(dataFlowAnalyzer::exitLeftBinaryOrArgument)
                    .transformRightOperand(this, ResolutionMode.WithExpectedType(booleanType)).also(dataFlowAnalyzer::exitBinaryOr)
        }.transformOtherChildren(transformer, ResolutionMode.WithExpectedType(booleanType)).also {
            it.resultType = booleanType
        }.compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        // val resolvedAssignment = transformCallee(variableAssignment)
        variableAssignment.annotations.forEach { it.accept(this, data) }
        val resolvedAssignment = callResolver.resolveVariableAccessAndSelectCandidate(variableAssignment, file)
        val result = if (resolvedAssignment is FirVariableAssignment) {
            val completeAssignment = callCompleter.completeCall(resolvedAssignment, noExpectedType) // TODO: check
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
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        if (callableReferenceAccess.calleeReference is FirResolvedNamedReference) {
            return callableReferenceAccess.compose()
        }

        callableReferenceAccess.annotations.forEach { it.accept(this, data) }
        val transformedLHS =
            callableReferenceAccess.explicitReceiver?.transformSingle(this, ResolutionMode.ContextIndependent)

        val callableReferenceAccessWithTransformedLHS =
            if (transformedLHS != null)
                callableReferenceAccess.transformExplicitReceiver(StoreReceiver, transformedLHS)
            else
                callableReferenceAccess

        if (data !is ResolutionMode.ContextDependent) {
            val resolvedReference =
                components.syntheticCallGenerator.resolveCallableReferenceWithSyntheticOuterCall(
                    callableReferenceAccess, data.expectedType
                ) ?: callableReferenceAccess

            return resolvedReference.compose()
        }

        return callableReferenceAccessWithTransformedLHS.compose()
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        val transformedGetClassCall = transformExpression(getClassCall, data).single as FirGetClassCall
        val kClassSymbol = ClassId.fromString("kotlin/reflect/KClass")(session.firSymbolProvider)

        val typeOfExpression = when (val lhs = transformedGetClassCall.argument) {
            is FirResolvedQualifier -> {
                val classId = lhs.classId
                classId?.let { classId ->
                    val symbol = symbolProvider.getClassLikeSymbolByFqName(classId)
                    // TODO: Unify logic?
                    symbol?.constructType(
                        Array((symbol.phasedFir as? FirTypeParametersOwner)?.typeParameters?.size ?: 0) {
                            ConeStarProjection
                        },
                        isNullable = false
                    )
                } ?: lhs.resultType.coneTypeUnsafe<ConeKotlinType>()
            }
            else -> lhs.resultType.coneTypeUnsafe<ConeKotlinType>()
        }

        transformedGetClassCall.resultType =
            FirResolvedTypeRefImpl(
                null,
                kClassSymbol.constructType(arrayOf(typeOfExpression), false)
            )
        return transformedGetClassCall.compose()
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        transformExpression(wrappedDelegateExpression, data)
        with(wrappedDelegateExpression) {
            val delegateProviderTypeRef = delegateProvider.typeRef
            val useDelegateProvider = delegateProviderTypeRef is FirResolvedTypeRef &&
                    delegateProviderTypeRef !is FirErrorTypeRef &&
                    delegateProviderTypeRef.type !is ConeKotlinErrorType
            return if (useDelegateProvider) delegateProvider.compose() else expression.compose()
        }
    }

    override fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        constExpression.annotations.forEach { it.accept(this, data) }
        val kind = constExpression.kind
        val symbol = when (kind) {
            FirConstKind.Null -> StandardClassIds.Nothing(symbolProvider)
            FirConstKind.Boolean -> StandardClassIds.Boolean(symbolProvider)
            FirConstKind.Char -> StandardClassIds.Char(symbolProvider)
            FirConstKind.Byte -> StandardClassIds.Byte(symbolProvider)
            FirConstKind.Short -> StandardClassIds.Short(symbolProvider)
            FirConstKind.Int -> StandardClassIds.Int(symbolProvider)
            FirConstKind.Long -> StandardClassIds.Long(symbolProvider)
            FirConstKind.String -> StandardClassIds.String(symbolProvider)
            FirConstKind.Float -> StandardClassIds.Float(symbolProvider)
            FirConstKind.Double -> StandardClassIds.Double(symbolProvider)
            FirConstKind.IntegerLiteral -> null
        }

        val type = if (symbol != null) {
            ConeClassLikeTypeImpl(symbol.toLookupTag(), emptyArray(), isNullable = kind == FirConstKind.Null)
        } else {
            val integerLiteralType = ConeIntegerLiteralTypeImpl(constExpression.value as Long)
            val expectedType = data.expectedType?.coneTypeSafe<ConeKotlinType>()
            if (expectedType != null) {
                val approximatedType = integerLiteralType.getApproximatedType(expectedType)
                val newConstKind = approximatedType.toConstKind()
                if (newConstKind == null) {
                    constExpression.replaceKind(FirConstKind.Int as FirConstKind<T>)
                    dataFlowAnalyzer.exitConstExpresion(constExpression as FirConstExpression<*>)
                    constExpression.resultType = FirErrorTypeRefImpl(
                        constExpression.source,
                        FirTypeMismatchError(expectedType, integerLiteralType.getApproximatedType())
                    )
                    return constExpression.compose()
                }
                constExpression.replaceKind(newConstKind as FirConstKind<T>)
                approximatedType
            } else {
                integerLiteralType
            }
        }
        dataFlowAnalyzer.exitConstExpresion(constExpression as FirConstExpression<*>)
        constExpression.resultType = constExpression.resultType.resolvedTypeFromPrototype(type)
        return constExpression.compose()
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterAnnotationCall(annotationCall)
        return (annotationCall.transformChildren(transformer, data) as FirAnnotationCall)
//            TODO: it's temporary incorrect solution until we design resolve and completion for annotation calls
            .transformArguments(integerLiteralTypeApproximator, null).also {
                dataFlowAnalyzer.exitAnnotationCall(it)
            }.compose()
    }

    private fun ConeKotlinTypeProjection.toFirTypeProjection(): FirTypeProjection = when (this) {
        is ConeStarProjection -> FirStarProjectionImpl(null)
        else -> {
            val variance = when (kind) {
                ProjectionKind.IN -> Variance.IN_VARIANCE
                ProjectionKind.OUT -> Variance.OUT_VARIANCE
                ProjectionKind.INVARIANT -> Variance.INVARIANT
                ProjectionKind.STAR -> throw IllegalStateException()
            }
            val type = when (this) {
                is ConeKotlinTypeProjectionIn -> type
                is ConeKotlinTypeProjectionOut -> type
                is ConeStarProjection -> throw IllegalStateException()
                else -> this as ConeKotlinType
            }
            FirTypeProjectionWithVarianceImpl(
                null, FirResolvedTypeRefImpl(null, type), variance
            )
        }
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        if (transformer.implicitTypeOnly) return delegatedConstructorCall.compose()
        delegatedConstructorCall.transformChildren(transformer, ResolutionMode.ContextDependent)
        val typeArguments: List<FirTypeProjection>
        val symbol: FirClassSymbol<*> = when (val reference = delegatedConstructorCall.calleeReference) {
            is FirThisReference -> {
                typeArguments = emptyList()
                if (reference.boundSymbol == null) {
                    implicitReceiverStack.lastDispatchReceiver()?.boundSymbol?.also {
                        reference.replaceBoundSymbol(it)
                    } ?: return delegatedConstructorCall.compose()
                } else {
                    reference.boundSymbol!! as FirClassSymbol<*>
                }
            }
            is FirSuperReference -> {
                // TODO: unresolved supertype
                val supertype = reference.superTypeRef.coneTypeSafe<ConeClassLikeType>() ?: return delegatedConstructorCall.compose()
                typeArguments = supertype.typeArguments.takeIf { it.isNotEmpty() }?.map { it.toFirTypeProjection() } ?: emptyList()
                val expandedSupertype = supertype.fullyExpandedType(session)
                val lookupTag = expandedSupertype.lookupTag
                if (lookupTag is ConeClassLookupTagWithFixedSymbol) {
                    lookupTag.symbol
                } else {
                    // TODO: support locals
                    symbolProvider.getSymbolByLookupTag(lookupTag) ?: return delegatedConstructorCall.compose()
                } as FirClassSymbol<*>
            }
            else -> return delegatedConstructorCall.compose()
        }
        val result = callResolver.resolveDelegatingConstructorCall(delegatedConstructorCall, symbol, typeArguments) ?: return delegatedConstructorCall.compose()
        return callCompleter.completeCall(result, noExpectedType).compose()
    }

    // ------------------------------------------------------------------------------------------------

    internal fun <T> storeTypeFromCallee(access: T) where T : FirQualifiedAccess, T : FirExpression {
        access.resultType = callCompleter.typeFromCallee(access)
    }
}
