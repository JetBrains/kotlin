/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildNamedArgumentExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedCallableReferenceTarget
import org.jetbrains.kotlin.fir.resolve.inference.extractInputOutputTypesFromCallableReferenceExpectedType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.runTransaction
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment


internal object CheckCallableReferenceExpectedType : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val outerCsBuilder = callInfo.outerCSBuilder ?: return
        val expectedType = callInfo.expectedType
        if (candidate.symbol !is FirCallableSymbol<*>) return

        val resultingReceiverType = when (callInfo.lhs) {
            is DoubleColonLHS.Type -> callInfo.lhs.type.takeIf { callInfo.explicitReceiver !is FirResolvedQualifier }
            else -> null
        }

        val fir: FirCallableDeclaration = candidate.symbol.fir as FirCallableDeclaration

        val isExpectedTypeReflectionType = callInfo.expectedType?.isReflectFunctionType(callInfo.session) == true
        val (rawResultingType, callableReferenceAdaptation) = buildResultingTypeAndAdaptation(
            fir,
            resultingReceiverType,
            candidate,
            context,
            // If the input and output types match the expected type but the expected type is a reflection type, and we need an adaptation,
            // we want to report AdaptedCallableReferenceIsUsedWithReflection but *not* InapplicableCandidate because
            // AdaptedCallableReferenceIsUsedWithReflection has the higher applicability.
            // Therefore, we force a reflection type whenever the expected type is a reflection type.
            //
            // If the input and output types end up not matching, we'll report InapplicableCandidate, regardless of whether the
            // expected/actual type is a reflection type.
            forceReflectionType = isExpectedTypeReflectionType
        )
        val resultingType = candidate.substitutor.substituteOrSelf(rawResultingType)

        if (callableReferenceAdaptation != null) {
            if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) {
                sink.reportDiagnostic(LowerPriorityToPreserveCompatibilityDiagnostic)
            }

            if (isExpectedTypeReflectionType) {
                sink.reportDiagnostic(AdaptedCallableReferenceIsUsedWithReflection)
            }
        }

        candidate.resultingTypeForCallableReference = resultingType
        candidate.callableReferenceAdaptation = callableReferenceAdaptation
        candidate.outerConstraintBuilderEffect = fun ConstraintSystemOperation.() {
            addOtherSystem(candidate.system.currentStorage())

            // Callable references are either arguments to a call or are wrapped in a synthetic call for resolution.
            val position = ConeArgumentConstraintPosition(callInfo.callSite)

            if (expectedType != null && !resultingType.contains {
                    it is ConeTypeVariableType && it.typeConstructor !in outerCsBuilder.currentStorage().allTypeVariables
                }
            ) {
                addSubtypeConstraint(resultingType, expectedType, position)
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

/**
 *  The resulting type is a reflection type ([FunctionTypeKind.reflectKind])
 *  iff the adaptation is `null` or [forceReflectionType]` == true`.
 *  Otherwise, it's a non-reflection type ([FunctionTypeKind.nonReflectKind]).
 */
private fun buildResultingTypeAndAdaptation(
    fir: FirCallableDeclaration,
    receiverType: ConeKotlinType?,
    candidate: Candidate,
    context: ResolutionContext,
    forceReflectionType: Boolean,
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
            if (fir.receiverParameter == null && receiverType != null) {
                parameters += receiverType
            }

            val returnTypeWithoutCoercion = returnTypeRef.coneType
            val returnType = if (callableReferenceAdaptation == null) {
                returnTypeWithoutCoercion.also {
                    fir.valueParameters.mapTo(parameters) { it.returnTypeRef.coneType }
                }
            } else {
                parameters += callableReferenceAdaptation.argumentTypes
                // K1 simply doesn't perform any conversions for so-called "top level callable references",
                // only for "callable reference arguments"
                // (see CallableReferencesCandidateFactory.buildReflectionType, val buildTypeWithConversions)
                // Here hasSyntheticOuterCall is ~ an equivalent of "top level callable references" in K1
                // K2 behavior differs at least in two aspects:
                // - it still allows to drop some default parameters / convert some varargs
                //     - see testData/diagnostics/tests/callableReference/adapted/simpleAdaptationOutsideOfCall.kt
                //     - see testData/diagnostics/tests/callableReference/resolve/withVararg.kt
                // - coercion to unit is still allowed if a candidate return type is not type parameter based
                //     - see testData/diagnostics/tests/inference/callableReferences/conversionLastStatementInLambda.kt
                val hasSyntheticOuterCall = candidate.callInfo.hasSyntheticOuterCall
                if (callableReferenceAdaptation.coercionStrategy != CoercionStrategy.COERCION_TO_UNIT ||
                    hasSyntheticOuterCall && returnTypeWithoutCoercion.unwrapFlexibleAndDefinitelyNotNull() is ConeTypeParameterType
                ) {
                    returnTypeWithoutCoercion
                } else {
                    context.session.builtinTypes.unitType.type
                }
            }

            val baseFunctionTypeKind = callableReferenceAdaptation?.suspendConversionStrategy?.kind
                ?: fir.specialFunctionTypeKind(context.session)
                ?: FunctionTypeKind.Function

            return createFunctionType(
                if (callableReferenceAdaptation == null || forceReflectionType) baseFunctionTypeKind.reflectKind() else baseFunctionTypeKind.nonReflectKind(),
                parameters,
                receiverType = receiverType.takeIf { fir.receiverParameter != null },
                rawReturnType = returnType,
                contextReceivers = fir.contextReceivers.map { it.typeRef.coneType }
            ) to callableReferenceAdaptation
        }
        is FirVariable -> {
            val returnType = returnTypeRef.type
            val isMutable = fir.canBeMutableReference(candidate)
            val propertyType = when {
                isMutable && returnType.hasCapture() ->
                    // capturing types in mutable property references is unsound in general
                    context.inferenceComponents.resultTypeResolver.typeApproximator
                        .approximateToSuperType(returnType, TypeApproximatorConfiguration.InternalTypesApproximation) as? ConeKotlinType
                        ?: returnType
                else -> returnType
            }
            createKPropertyType(receiverType, propertyType, isMutable) to null
        }
        else -> ConeErrorType(ConeUnsupportedCallableReferenceTarget(candidate)) to null
    }
}

internal class CallableReferenceAdaptation(
    val argumentTypes: Array<ConeKotlinType>,
    val coercionStrategy: CoercionStrategy,
    val defaults: Int,
    val mappedArguments: CallableReferenceMappedArguments,
    val suspendConversionStrategy: CallableReferenceConversionStrategy,
) {
    init {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            require(
                defaults != 0 ||
                        suspendConversionStrategy != CallableReferenceConversionStrategy.NoConversion ||
                        coercionStrategy != CoercionStrategy.NO_COERCION ||
                        mappedArguments.values.any { it is ResolvedCallArgument.VarargArgument }
            ) {
                "Adaptation must be non-trivial."
            }
        }
    }
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
    val originScope = function.dispatchReceiverType?.scope(
        useSiteSession = session,
        scopeSession = scopeSession,
        callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
        requiredMembersPhase = FirResolvePhase.STATUS,
    )

    val argumentMapping = mapArguments(fakeArguments, function, originScope = originScope, callSiteIsOperatorCall = false)
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

    var isThereVararg = mappedVarargElements.isNotEmpty()
    for (valueParameter in function.valueParameters) {
        if (valueParameter.isVararg && valueParameter !in mappedArguments) {
            mappedArguments[valueParameter] = ResolvedCallArgument.VarargArgument(emptyList())
            isThereVararg = true
        }
    }

    val returnTypeRef = function.returnTypeRef
    val coercionStrategy =
        if (returnExpectedType.isUnitOrFlexibleUnit &&
            returnTypeRef.coneTypeSafe<ConeKotlinType>()?.fullyExpandedType(session)?.isUnit != true
        )
            CoercionStrategy.COERCION_TO_UNIT
        else
            CoercionStrategy.NO_COERCION

    val adaptedArguments = if (expectedType.isBaseTypeForNumberedReferenceTypes)
        emptyMap()
    else
        mappedArguments

    val expectedTypeFunctionKind = expectedType.functionTypeKind(session)?.takeUnless { it.isBasicFunctionOrKFunction }
    val functionKind = function.specialFunctionTypeKind(session)

    val conversionStrategy = if (expectedTypeFunctionKind != null && functionKind == null) {
        CallableReferenceConversionStrategy.CustomConversion(expectedTypeFunctionKind)
    } else {
        CallableReferenceConversionStrategy.NoConversion
    }

    if (defaults == 0 && !isThereVararg &&
        coercionStrategy == CoercionStrategy.NO_COERCION && conversionStrategy == CallableReferenceConversionStrategy.NoConversion
    ) {
        // Do not create adaptation for trivial (id) conversion as it makes resulting type FunctionN instead of KFunctionN
        // It happens because adapted references do not support reflection (see KT-40406)
        return null
    }

    @Suppress("UNCHECKED_CAST")
    return CallableReferenceAdaptation(
        mappedArgumentTypes as Array<ConeKotlinType>,
        coercionStrategy,
        defaults,
        adaptedArguments,
        conversionStrategy
    )
}



sealed class CallableReferenceConversionStrategy {
    abstract val kind: FunctionTypeKind?

    object NoConversion : CallableReferenceConversionStrategy() {
        override val kind: FunctionTypeKind?
            get() = null
    }

    class CustomConversion(override val kind: FunctionTypeKind) : CallableReferenceConversionStrategy()
}

private fun varargParameterTypeByExpectedParameter(
    expectedParameterType: ConeKotlinType,
    substitutedParameter: FirValueParameter,
    varargMappingState: VarargMappingState,
): Pair<ConeKotlinType?, VarargMappingState> {
    val elementType = substitutedParameter.returnTypeRef.coneType.arrayElementType()
        ?: errorWithAttachment("Vararg parameter ${substitutedParameter::class.java} does not have vararg type") {
            withConeTypeEntry("expectedParameterType", expectedParameterType)
            withFirEntry("substitutedParameter", substitutedParameter)
            withEntry("varargMappingState", varargMappingState.toString())
        }

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
    val index: Int,
) : FirExpression() {
    override val source: KtSourceElement?
        get() = null

    @UnresolvedExpressionTypeAccess
    override val coneTypeOrNull: ConeKotlinType
        get() = shouldNotBeCalled()

    override val annotations: List<FirAnnotation>
        get() = shouldNotBeCalled()

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        shouldNotBeCalled()
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        shouldNotBeCalled()
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirNamedArgumentExpression {
        shouldNotBeCalled()
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        shouldNotBeCalled()
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        shouldNotBeCalled()
    }
}

fun ConeKotlinType.isKCallableType(): Boolean {
    return this.classId == StandardClassIds.KCallable
}

private fun FirVariable.canBeMutableReference(candidate: Candidate): Boolean {
    if (!isVar) return false
    if (this is FirField) return true
    val original = this.unwrapFakeOverridesOrDelegated()
    return original.source?.kind == KtFakeSourceElementKind.PropertyFromParameter ||
            (original.setter is FirMemberDeclaration &&
                    candidate.callInfo.session.visibilityChecker.isVisible(original.setter!!, candidate))
}
