/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.builder.buildNamedArgumentExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedCallableReferenceTarget
import org.jetbrains.kotlin.fir.resolve.inference.extractInputOutputTypesFromCallableReferenceExpectedType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.components.SuspendConversionStrategy
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.expressions.CoercionStrategy


internal object CheckCallableReferenceExpectedType : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val outerCsBuilder = callInfo.outerCSBuilder ?: return
        val expectedType = callInfo.expectedType
        if (candidate.symbol !is FirCallableSymbol<*>) return

        val resultingReceiverType = when (callInfo.lhs) {
            is DoubleColonLHS.Type -> callInfo.lhs.type.takeIf { callInfo.explicitReceiver !is FirResolvedQualifier }
            else -> null
        }

        val fir: FirCallableDeclaration = candidate.symbol.fir

        val (rawResultingType, callableReferenceAdaptation) = buildReflectionType(fir, resultingReceiverType, candidate, context)
        val resultingType = candidate.substitutor.substituteOrSelf(rawResultingType)

        if (callableReferenceAdaptation.needCompatibilityResolveForCallableReference()) {
            sink.reportDiagnostic(LowerPriorityToPreserveCompatibilityDiagnostic)
        }

        candidate.resultingTypeForCallableReference = resultingType
        candidate.callableReferenceAdaptation = callableReferenceAdaptation
        candidate.outerConstraintBuilderEffect = fun ConstraintSystemOperation.() {
            addOtherSystem(candidate.system.asReadOnlyStorage())

            val position = SimpleConstraintSystemConstraintPosition //TODO

            if (expectedType != null) {
                addSubtypeConstraint(resultingType, expectedType, position)
            }

            val declarationReceiverType: ConeKotlinType? =
                fir.receiverTypeRef?.coneType
                    ?.let(candidate.substitutor::substituteOrSelf)

            if (resultingReceiverType != null && declarationReceiverType != null) {
                val capturedReceiver = context.inferenceComponents.ctx.captureFromExpression(resultingReceiverType) ?: resultingReceiverType
                addSubtypeConstraint(capturedReceiver, declarationReceiverType, position)
            }
        }

        var isApplicable = true

        outerCsBuilder.runTransaction {
            candidate.outerConstraintBuilderEffect!!(this)

            isApplicable = !hasContradiction

            false
        }

        if (!isApplicable) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }
}

private fun buildReflectionType(
    fir: FirCallableDeclaration,
    receiverType: ConeKotlinType?,
    candidate: Candidate,
    context: ResolutionContext
): Pair<ConeKotlinType, CallableReferenceAdaptation?> {
    val returnTypeRef = context.bodyResolveComponents.returnTypeCalculator.tryCalculateReturnType(fir)
    return when (fir) {
        is FirFunction -> {
            val unboundReferenceTarget = if (receiverType != null) 1 else 0
            val callableReferenceAdaptation =
                context.bodyResolveComponents.getCallableReferenceAdaptation(
                    context.session,
                    fir,
                    candidate.callInfo.expectedType?.lowerBoundIfFlexible(),
                    unboundReferenceTarget
                )

            val parameters = mutableListOf<ConeKotlinType>()

            val returnType = callableReferenceAdaptation?.let {
                parameters += it.argumentTypes
                if (it.coercionStrategy == CoercionStrategy.COERCION_TO_UNIT) {
                    context.session.builtinTypes.unitType.type
                } else {
                    returnTypeRef.coneType
                }
            } ?: returnTypeRef.coneType.also {
                fir.valueParameters.mapTo(parameters) { it.returnTypeRef.coneType }
            }

            val isSuspend = (fir as? FirSimpleFunction)?.isSuspend == true ||
                    callableReferenceAdaptation?.suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION
            return createFunctionalType(
                parameters,
                receiverType = receiverType,
                rawReturnType = returnType,
                isKFunctionType = true,
                isSuspend = isSuspend
            ) to callableReferenceAdaptation
        }
        is FirVariable -> createKPropertyType(fir, receiverType, returnTypeRef, candidate) to null
        else -> ConeClassErrorType(ConeUnsupportedCallableReferenceTarget(fir)) to null
    }
}

internal class CallableReferenceAdaptation(
    val argumentTypes: Array<ConeKotlinType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: CallableReferenceMappedArguments,
    val suspendConversionStrategy: SuspendConversionStrategy
)

private fun CallableReferenceAdaptation?.needCompatibilityResolveForCallableReference(): Boolean {
    // KT-13934: check containing declaration for companion object
    if (this == null) return false
    return defaults != 0 ||
            suspendConversionStrategy != SuspendConversionStrategy.NO_CONVERSION ||
            coercionStrategy != CoercionStrategy.NO_COERCION ||
            mappedArguments.values.any { it is ResolvedCallArgument.VarargArgument }
}

private fun BodyResolveComponents.getCallableReferenceAdaptation(
    session: FirSession,
    function: FirFunction,
    expectedType: ConeKotlinType?,
    unboundReceiverCount: Int
): CallableReferenceAdaptation? {
    if (expectedType == null) return null

    // Do not adapt references against KCallable type as it's impossible to map defaults/vararg to absent parameters of KCallable
    if (expectedType.isKCallableType()) return null

    val (inputTypes, returnExpectedType) = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session) ?: return null
    val expectedArgumentsCount = inputTypes.size - unboundReceiverCount
    if (expectedArgumentsCount < 0) return null

    val fakeArguments = createFakeArgumentsForReference(function, expectedArgumentsCount, inputTypes, unboundReceiverCount)
    // TODO: Use correct originScope
    val argumentMapping = mapArguments(fakeArguments, function, originScope = null)
    if (argumentMapping.diagnostics.any { !it.applicability.isSuccess }) return null

    /**
     * (A, B, C) -> Unit
     * fun foo(a: A, b: B = B(), vararg c: C)
     */
    var defaults = 0
    var varargMappingState = VarargMappingState.UNMAPPED
    val mappedArguments = linkedMapOf<FirValueParameter, ResolvedCallArgument>()
    val mappedVarargElements = linkedMapOf<FirValueParameter, MutableList<FirExpression>>()
    val mappedArgumentTypes = arrayOfNulls<ConeKotlinType?>(fakeArguments.size)

    for ((valueParameter, resolvedArgument) in argumentMapping.parameterToCallArgumentMap) {
        for (fakeArgument in resolvedArgument.arguments) {
            val index = fakeArgument.index
            val substitutedParameter = function.valueParameters.getOrNull(function.indexOf(valueParameter)) ?: continue

            val mappedArgument: ConeKotlinType?
            if (substitutedParameter.isVararg) {
                val (varargType, newVarargMappingState) = varargParameterTypeByExpectedParameter(
                    inputTypes[index + unboundReceiverCount],
                    substitutedParameter,
                    varargMappingState
                )
                varargMappingState = newVarargMappingState
                mappedArgument = varargType

                when (newVarargMappingState) {
                    VarargMappingState.MAPPED_WITH_ARRAY -> {
                        // If we've already mapped an argument to this value parameter, it'll always be a type mismatch.
                        mappedArguments[valueParameter] = ResolvedCallArgument.SimpleArgument(fakeArgument)
                    }
                    VarargMappingState.MAPPED_WITH_PLAIN_ARGS -> {
                        mappedVarargElements.getOrPut(valueParameter) { ArrayList() }.add(fakeArgument)
                    }
                    VarargMappingState.UNMAPPED -> {
                    }
                }
            } else {
                mappedArgument = substitutedParameter.returnTypeRef.coneType
                mappedArguments[valueParameter] = resolvedArgument
            }

            mappedArgumentTypes[index] = mappedArgument
        }
        if (resolvedArgument == ResolvedCallArgument.DefaultArgument) {
            defaults++
            mappedArguments[valueParameter] = resolvedArgument
        }
    }
    if (mappedArgumentTypes.any { it == null }) return null

    for ((valueParameter, varargElements) in mappedVarargElements) {
        mappedArguments[valueParameter] = ResolvedCallArgument.VarargArgument(varargElements)
    }

    for (valueParameter in function.valueParameters) {
        if (valueParameter.isVararg && valueParameter !in mappedArguments) {
            mappedArguments[valueParameter] = ResolvedCallArgument.VarargArgument(emptyList())
        }
    }

    val coercionStrategy = if (returnExpectedType.isUnitOrFlexibleUnit && !function.returnTypeRef.isUnit)
        CoercionStrategy.COERCION_TO_UNIT
    else
        CoercionStrategy.NO_COERCION

    val adaptedArguments = if (expectedType.isBaseTypeForNumberedReferenceTypes)
        emptyMap()
    else
        mappedArguments

    val suspendConversionStrategy = if ((function as? FirSimpleFunction)?.isSuspend != true && expectedType.isSuspendFunctionType(session))
        SuspendConversionStrategy.SUSPEND_CONVERSION
    else
        SuspendConversionStrategy.NO_CONVERSION

    @Suppress("UNCHECKED_CAST")
    return CallableReferenceAdaptation(
        mappedArgumentTypes as Array<ConeKotlinType>,
        coercionStrategy,
        defaults,
        adaptedArguments,
        suspendConversionStrategy
    )
}

fun ConeKotlinType?.isPotentiallyArray(): Boolean =
    this != null && (this.arrayElementType() != null || this is ConeTypeVariableType)

private fun varargParameterTypeByExpectedParameter(
    expectedParameterType: ConeKotlinType,
    substitutedParameter: FirValueParameter,
    varargMappingState: VarargMappingState,
): Pair<ConeKotlinType?, VarargMappingState> {
    val elementType = substitutedParameter.returnTypeRef.coneType.arrayElementType()
        ?: error("Vararg parameter $substitutedParameter does not have vararg type")

    return when (varargMappingState) {
        VarargMappingState.UNMAPPED -> {
            if (expectedParameterType.isPotentiallyArray()) {
                elementType.createOutArrayType() to VarargMappingState.MAPPED_WITH_ARRAY
            } else {
                elementType to VarargMappingState.MAPPED_WITH_PLAIN_ARGS
            }
        }
        VarargMappingState.MAPPED_WITH_PLAIN_ARGS -> {
            if (expectedParameterType.isPotentiallyArray())
                null to VarargMappingState.MAPPED_WITH_PLAIN_ARGS
            else
                elementType to VarargMappingState.MAPPED_WITH_PLAIN_ARGS
        }
        VarargMappingState.MAPPED_WITH_ARRAY ->
            null to VarargMappingState.MAPPED_WITH_ARRAY
    }
}


private enum class VarargMappingState {
    UNMAPPED, MAPPED_WITH_PLAIN_ARGS, MAPPED_WITH_ARRAY
}

private fun FirFunction.indexOf(valueParameter: FirValueParameter): Int = valueParameters.indexOf(valueParameter)

val ConeKotlinType.isUnitOrFlexibleUnit: Boolean
    get() {
        val type = this.lowerBoundIfFlexible()
        if (type.isNullable) return false
        val classId = type.classId ?: return false
        return classId == StandardClassIds.Unit
    }

private val ConeKotlinType.isBaseTypeForNumberedReferenceTypes: Boolean
    get() {
        val classId = lowerBoundIfFlexible().classId ?: return false
        return when (classId) {
            StandardClassIds.KProperty,
            StandardClassIds.KMutableProperty,
            StandardClassIds.KCallable -> true
            else -> false
        }
    }

private val FirExpression.index: Int
    get() = when (this) {
        is FirNamedArgumentExpression -> expression.index
        is FirFakeArgumentForCallableReference -> index
        else -> throw IllegalArgumentException()
    }

private fun createFakeArgumentsForReference(
    function: FirFunction,
    expectedArgumentCount: Int,
    inputTypes: List<ConeKotlinType>,
    unboundReceiverCount: Int
): List<FirExpression> {
    var afterVararg = false
    var varargComponentType: ConeKotlinType? = null
    var vararg = false
    return (0 until expectedArgumentCount).map { index ->
        val inputType = inputTypes.getOrNull(index + unboundReceiverCount)
        if (vararg && varargComponentType != inputType) {
            afterVararg = true
        }

        val valueParameter = function.valueParameters.getOrNull(index)
        val name = if (afterVararg && valueParameter?.defaultValue != null)
            valueParameter.name
        else
            null

        if (valueParameter?.isVararg == true) {
            varargComponentType = inputType
            vararg = true
        }
        if (name != null) {
            buildNamedArgumentExpression {
                expression = FirFakeArgumentForCallableReference(index)
                this.name = name
                isSpread = false
            }
        } else {
            FirFakeArgumentForCallableReference(index)
        }
    }
}

class FirFakeArgumentForCallableReference(
    val index: Int
) : FirExpression() {
    override val source: FirSourceElement?
        get() = null

    override val typeRef: FirTypeRef
        get() = error("should not be called")

    override val annotations: List<FirAnnotationCall>
        get() = error("should not be called")

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        error("should not be called")
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirNamedArgumentExpression {
        error("should not be called")
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        error("should not be called")
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        error("should not be called")
    }
}

fun ConeKotlinType.isKCallableType(): Boolean {
    return this.classId == StandardClassIds.KCallable
}

private fun createKPropertyType(
    propertyOrField: FirVariable,
    receiverType: ConeKotlinType?,
    returnTypeRef: FirResolvedTypeRef,
    candidate: Candidate,
): ConeKotlinType {
    val propertyType = returnTypeRef.type
    return org.jetbrains.kotlin.fir.resolve.createKPropertyType(
        receiverType,
        propertyType,
        isMutable = propertyOrField.canBeMutableReference(candidate)
    )
}

private fun FirVariable.canBeMutableReference(candidate: Candidate): Boolean {
    if (!isVar) return false
    if (this is FirField) return true
    val original = this.unwrapFakeOverrides()
    return original.source?.kind == FirFakeSourceElementKind.PropertyFromParameter ||
            (original.setter is FirMemberDeclaration &&
                    candidate.callInfo.session.visibilityChecker.isVisible(original.setter!!, candidate))
}
