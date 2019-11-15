/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirErrorExpressionImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirVariableAssignmentImpl
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirErrorNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirOperatorAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirVariableExpectedError
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.StoreReceiver
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirExpressionsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private val callResolver: FirCallResolver get() = components.callResolver
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes

    init {
        components.callResolver.initTransformer(this)
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (expression.resultType is FirImplicitTypeRef && expression !is FirWrappedExpression) {
            val type = FirErrorTypeRefImpl(expression.source, FirSimpleDiagnostic("Type calculating for ${expression::class} is not supported", DiagnosticKind.InferenceError))
            expression.resultType = type
        }
        return (expression.transformChildren(transformer, data) as FirStatement).compose()
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
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
            val result = (operatorCall.transformChildren(transformer, ResolutionMode.ContextIndependent) as FirOperatorCall).also {
                it.resultType = operatorCall.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)
            }
            dataFlowAnalyzer.exitOperatorCall(result)
            return result.compose()
        }

        if (operatorCall.operation in FirOperation.ASSIGNMENTS) {
            require(operatorCall.operation != FirOperation.ASSIGN)
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

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        val symbolProvider = session.firSymbolProvider
        val resolved = transformExpression(typeOperatorCall, data).single
        when ((resolved as FirTypeOperatorCall).operation) {
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
        return resolved.compose()
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
        val resolvedAssignment = callResolver.resolveVariableAccessAndSelectCandidate(variableAssignment, file)
        val result = if (resolvedAssignment is FirVariableAssignment) {
            val completeAssignment = callCompleter.completeCall(resolvedAssignment, noExpectedType) // TODO: check
            val expectedType = components.typeFromCallee(completeAssignment)
            completeAssignment.transformRValue(transformer, withExpectedType(expectedType))
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
        // TODO: add support of IntegerLiteralType

        val kind = constExpression.kind
        val symbol = when (kind) {
            IrConstKind.Null -> StandardClassIds.Nothing(symbolProvider)
            IrConstKind.Boolean -> StandardClassIds.Boolean(symbolProvider)
            IrConstKind.Char -> StandardClassIds.Char(symbolProvider)
            IrConstKind.Byte -> StandardClassIds.Byte(symbolProvider)
            IrConstKind.Short -> StandardClassIds.Short(symbolProvider)
            IrConstKind.Int -> StandardClassIds.Int(symbolProvider)
            IrConstKind.Long -> StandardClassIds.Long(symbolProvider)
            IrConstKind.String -> StandardClassIds.String(symbolProvider)
            IrConstKind.Float -> StandardClassIds.Float(symbolProvider)
            IrConstKind.Double -> StandardClassIds.Double(symbolProvider)
        }

        val type = ConeClassLikeTypeImpl(symbol.toLookupTag(), emptyArray(), isNullable = kind == IrConstKind.Null)

        constExpression.resultType = FirResolvedTypeRefImpl(null, type)
        dataFlowAnalyzer.exitConstExpresion(constExpression as FirConstExpression<*>)

        return constExpression.compose()
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterAnnotationCall(annotationCall)
        return (annotationCall.transformChildren(transformer, data) as FirAnnotationCall).also {
            dataFlowAnalyzer.exitAnnotationCall(it)
        }.compose()
    }

    // ------------------------------------------------------------------------------------------------

    internal fun <T> storeTypeFromCallee(access: T) where T : FirQualifiedAccess, T : FirExpression {
        access.resultType = callCompleter.typeFromCallee(access)
    }
}
