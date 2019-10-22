/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionWithSmartcastImpl
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.typeFromCallee
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.ClassId

class FirExpressionsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private val qualifiedResolver: FirQualifiedNameResolver = FirQualifiedNameResolver(components)
    private val callResolver: FirCallResolver = FirCallResolver(
        this,
        topLevelScopes,
        localScopes,
        implicitReceiverStack,
        qualifiedResolver
    )

    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes

    override fun transformExpression(expression: FirExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (expression.resultType is FirImplicitTypeRef && expression !is FirWrappedExpression) {
            val type = FirErrorTypeRefImpl(expression.psi, "Type calculating for ${expression::class} is not supported")
            expression.resultType = type
        }
        return (expression.transformChildren(transformer, data) as FirStatement).compose()
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Any?
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
                if (callee.superTypeRef is FirResolvedTypeRef) {
                    qualifiedAccessExpression.resultType = callee.superTypeRef
                } else {
                    val superTypeRef = implicitReceiverStack.lastDispatchReceiver()
                        ?.boundSymbol?.phasedFir?.superTypeRefs?.firstOrNull()
                        ?: FirErrorTypeRefImpl(qualifiedAccessExpression.psi, "No super type")
                    qualifiedAccessExpression.resultType = superTypeRef
                    callee.replaceSuperTypeRef(superTypeRef)
                }
                qualifiedAccessExpression
            }
            is FirDelegateFieldReference -> {
                val delegateFieldSymbol = callee.resolvedSymbol
                qualifiedAccessExpression.resultType = delegateFieldSymbol.delegate.typeRef
                qualifiedAccessExpression
            }
            is FirResolvedCallableReference -> {
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
                    callCompleter.completeCall(transformedCallee, data as? FirTypeRef)
                } else {
                    transformedCallee
                }
            }
        }
        if (result is FirQualifiedAccessExpression) {
            result = transformQualifiedAccessUsingSmartcastInfo(result)
            dataFlowAnalyzer.exitQualifiedAccessExpression(result)
        }
        return result.compose()
    }

    private fun transformQualifiedAccessUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): FirQualifiedAccessExpression {
        val typesFromSmartCast = dataFlowAnalyzer.getTypeUsingSmartcastInfo(qualifiedAccessExpression) ?: return qualifiedAccessExpression
        val allTypes = typesFromSmartCast.toMutableList().also {
            it += qualifiedAccessExpression.resultType.coneTypeUnsafe<ConeKotlinType>()
        }
        val intersectedType = ConeTypeIntersector.intersectTypes(inferenceComponents.ctx as ConeInferenceContext, allTypes)
        // TODO: add check that intersectedType is not equal to original type
        val intersectedTypeRef = FirResolvedTypeRefImpl(qualifiedAccessExpression.resultType.psi, intersectedType).apply {
            annotations += qualifiedAccessExpression.resultType.annotations
        }
        return FirExpressionWithSmartcastImpl(qualifiedAccessExpression, intersectedTypeRef, typesFromSmartCast)
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Any?): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterFunctionCall(functionCall)
        if (functionCall.calleeReference is FirResolvedCallableReference && functionCall.resultType is FirImplicitTypeRef) {
            storeTypeFromCallee(functionCall)
        }
        if (functionCall.calleeReference !is FirSimpleNamedReference) return functionCall.compose()
        functionCall.transform<FirFunctionCall, InvocationKind?>(InvocationKindTransformer, null)
        val expectedTypeRef = data as FirTypeRef?
        val completeInference =
            try {
                val initialExplicitReceiver = functionCall.explicitReceiver
                val resultExpression = callResolver.resolveCallAndSelectCandidate(functionCall, expectedTypeRef, file)
                val resultExplicitReceiver = resultExpression.explicitReceiver
                if (initialExplicitReceiver !== resultExplicitReceiver && resultExplicitReceiver is FirQualifiedAccess) {
                    // name.invoke() case
                    callCompleter.completeCall(resultExplicitReceiver, noExpectedType)
                }
                callCompleter.completeCall(resultExpression, expectedTypeRef)
            } catch (e: Throwable) {
                throw RuntimeException("While resolving call ${functionCall.render()}", e)
            }

        dataFlowAnalyzer.exitFunctionCall(completeInference)
        return completeInference.compose()
    }

    override fun transformBlock(block: FirBlock, data: Any?): CompositeTransformResult<FirStatement> {
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
            FirImplicitUnitTypeRef(block.psi)
        } else {
            (resultExpression.resultType as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(null, "No type for block")
        }
        dataFlowAnalyzer.exitBlock(block)
        return block.compose()
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        return transformQualifiedAccessExpression(thisReceiverExpression, data)
    }

    override fun transformOperatorCall(operatorCall: FirOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
        val result = if (operatorCall.operation in FirOperation.BOOLEANS) {
            (operatorCall.transformChildren(transformer, noExpectedType) as FirOperatorCall).also {
                it.resultType = builtinTypes.booleanType
            }
        } else {
            transformExpression(operatorCall, data).single
        } as FirOperatorCall
        dataFlowAnalyzer.exitOperatorCall(result)
        return result.compose()
    }

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
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
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        val booleanType = builtinTypes.booleanType
        return when (binaryLogicExpression.kind) {
            LogicOperationKind.AND ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryAnd)
                    .transformLeftOperand(this, booleanType).also(dataFlowAnalyzer::exitLeftBinaryAndArgument)
                    .transformRightOperand(this, booleanType).also(dataFlowAnalyzer::exitBinaryAnd)

            LogicOperationKind.OR ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryOr)
                    .transformLeftOperand(this, booleanType).also(dataFlowAnalyzer::exitLeftBinaryOrArgument)
                    .transformRightOperand(this, booleanType).also(dataFlowAnalyzer::exitBinaryOr)
        }.transformOtherChildren(transformer, booleanType).also {
            it.resultType = booleanType
        }.compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        // val resolvedAssignment = transformCallee(variableAssignment)
        val resolvedAssignment = callResolver.resolveVariableAccessAndSelectCandidate(variableAssignment, file)
        val result = if (resolvedAssignment is FirVariableAssignment) {
            val completeAssignment = callCompleter.completeCall(resolvedAssignment, noExpectedType)
            val expectedType = components.typeFromCallee(completeAssignment)
            completeAssignment.transformRValue(transformer, expectedType)
        } else {
            // This can happen in erroneous code only
            resolvedAssignment
        }
        // TODO: maybe replace with FirAbstractAssignment for performance?
        (result as? FirVariableAssignment)?.let { dataFlowAnalyzer.exitVariableAssignment(it) }
        return result.compose()
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: Any?): CompositeTransformResult<FirStatement> {
        val transformedGetClassCall = transformExpression(getClassCall, data).single as FirGetClassCall
        val kClassSymbol = ClassId.fromString("kotlin/reflect/KClass")(session.firSymbolProvider)

        val typeOfExpression = when (val lhs = transformedGetClassCall.argument) {
            is FirResolvedQualifier -> {
                val classId = lhs.classId
                if (classId != null) {
                    val symbol = symbolProvider.getClassLikeSymbolByFqName(classId)!!
                    // TODO: Unify logic?
                    symbol.constructType(
                        Array(symbol.phasedFir.typeParameters.size) {
                            ConeStarProjection
                        },
                        isNullable = false
                    )
                } else {
                    lhs.resultType.coneTypeUnsafe<ConeKotlinType>()
                }
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
        data: Any?
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

    override fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: Any?): CompositeTransformResult<FirStatement> {
        val expectedType = data as FirTypeRef?

        val kind = constExpression.kind
        if (expectedType == null || expectedType is FirImplicitTypeRef ||
            kind == IrConstKind.Null || kind == IrConstKind.Boolean || kind == IrConstKind.Char
        ) {
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

            val type = ConeClassTypeImpl(symbol.toLookupTag(), emptyArray(), isNullable = kind == IrConstKind.Null)

            constExpression.resultType = FirResolvedTypeRefImpl(null, type)
        } else {
            constExpression.resultType = if (kind != IrConstKind.Null) {
                expectedType.resolvedTypeFromPrototype(
                    expectedType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NOT_NULL)
                )
            } else {
                expectedType
            }
        }


        return transformExpression(constExpression, data).also {
            dataFlowAnalyzer.exitConstExpresion(it.single as FirConstExpression<*>)
        }
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): CompositeTransformResult<FirStatement> {
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