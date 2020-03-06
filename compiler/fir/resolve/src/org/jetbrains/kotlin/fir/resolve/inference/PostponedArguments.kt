/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.CandidateApplicability
import org.jetbrains.kotlin.fir.resolve.calls.isExtensionFunctionType
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Candidate.preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirAnonymousFunction,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef,
    forceResolution: Boolean = false,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null
): PostponedResolvedAtom {
    if (expectedType != null && !forceResolution && csBuilder.isTypeVariable(expectedType)) {
        return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType, expectedTypeRef, this)
    }

    val resolvedArgument =
        extractLambdaInfoFromFunctionalType(expectedType, expectedTypeRef, argument, returnTypeVariable, bodyResolveComponents, this)
            ?: extraLambdaInfo(expectedType, argument, csBuilder, bodyResolveComponents.session, this)

    if (expectedType != null) {
        // TODO: add SAM conversion processing
        val lambdaType = createFunctionalType(resolvedArgument.parameters, resolvedArgument.receiver, resolvedArgument.returnType)
        csBuilder.addSubtypeConstraint(lambdaType, expectedType, SimpleConstraintSystemConstraintPosition)
    }

    return resolvedArgument
}

fun Candidate.preprocessCallableReference(
    argument: FirCallableReferenceAccess,
    expectedType: ConeKotlinType
) {
    val lhs = bodyResolveComponents.doubleColonExpressionResolver.resolveDoubleColonLHS(argument)
    postponedAtoms += ResolvedCallableReferenceAtom(argument, expectedType, lhs, bodyResolveComponents.session)
}

fun ConeKotlinType.isBuiltinFunctionalType(session: FirSession): Boolean {
    return when (this) {
        is ConeClassLikeType -> fullyExpandedType(session).lookupTag.classId.asString().startsWith("kotlin/Function")
        else -> false
    }
}

fun ConeKotlinType.isSuspendFunctionType(session: FirSession): Boolean {
    return when (val type = this) {
        is ConeClassLikeType -> {
            val classId = type.fullyExpandedType(session).lookupTag.classId
            classId.packageFqName.asString() == "kotlin" && classId.relativeClassName.asString().startsWith("SuspendFunction")
        }
        else -> false
    }
}

fun ConeKotlinType.receiverType(expectedTypeRef: FirTypeRef, session: FirSession): ConeKotlinType? {
    if (isBuiltinFunctionalType(session) && expectedTypeRef.isExtensionFunctionType(session)) {
        return ((this as ConeClassLikeType).fullyExpandedType(session).typeArguments.first() as ConeKotlinTypeProjection).type
    }
    return null
}

fun ConeKotlinType.returnType(session: FirSession): ConeKotlinType? {
    require(this is ConeClassLikeType)
    val projection = fullyExpandedType(session).typeArguments.last()
    return (projection as? ConeKotlinTypeProjection)?.type
}

private fun ConeKotlinType.valueParameterTypesIncludingReceiver(session: FirSession): List<ConeKotlinType?> {
    require(this is ConeClassLikeType)
    return fullyExpandedType(session).typeArguments.dropLast(1).map {
        (it as? ConeKotlinTypeProjection)?.type
    }
}

private val FirAnonymousFunction.returnType get() = returnTypeRef.coneTypeSafe<ConeKotlinType>()
private val FirAnonymousFunction.receiverType get() = receiverTypeRef?.coneTypeSafe<ConeKotlinType>()


private fun extraLambdaInfo(
    expectedType: ConeKotlinType?,
    argument: FirAnonymousFunction,
    csBuilder: ConstraintSystemBuilder,
    session: FirSession,
    candidate: Candidate?
): ResolvedLambdaAtom {
    val isSuspend = expectedType?.isSuspendFunctionType(session) ?: false

    val isFunctionSupertype =
        expectedType != null && expectedType.lowerBoundIfFlexible()
            .isBuiltinFunctionalType(session)//isNotNullOrNullableFunctionSupertype(expectedType)

    val typeVariable = ConeTypeVariableForLambdaReturnType(argument, "_L")

    val receiverType = argument.receiverType
    val returnType =
        argument.returnType
            ?: expectedType?.typeArguments?.singleOrNull()?.safeAs<ConeKotlinTypeProjection>()?.type?.takeIf { isFunctionSupertype }
            ?: typeVariable.defaultType

    val nothingType = argument.session.builtinTypes.nothingType.type
    val parameters = argument.valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: nothingType
    }

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    return ResolvedLambdaAtom(
        argument,
        expectedType,
        isSuspend,
        receiverType,
        parameters,
        returnType,
        typeVariable.takeIf { newTypeVariableUsed },
        candidate
    )
}

internal fun extractLambdaInfoFromFunctionalType(
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef,
    argument: FirAnonymousFunction,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
    components: BodyResolveComponents,
    candidate: Candidate?
): ResolvedLambdaAtom? {
    val session = components.session
    if (expectedType == null) return null
    if (expectedType is ConeFlexibleType) {
        return extractLambdaInfoFromFunctionalType(expectedType.lowerBound, expectedTypeRef, argument, returnTypeVariable, components, candidate)
    }
    if (!expectedType.isBuiltinFunctionalType(session)) return null

    val receiverType = argument.receiverType ?: expectedType.receiverType(expectedTypeRef, session)
    val returnType = argument.returnType ?: expectedType.returnType(session) ?: return null
    val parameters = extractLambdaParameters(expectedType, argument, expectedTypeRef.isExtensionFunctionType(session), session)

    return ResolvedLambdaAtom(
        argument,
        expectedType,
        expectedType.isSuspendFunctionType(session),
        receiverType,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        candidate
    )
}

private fun extractLambdaParameters(
    expectedType: ConeKotlinType,
    argument: FirAnonymousFunction,
    expectedTypeIsExtensionFunctionType: Boolean,
    session: FirSession
): List<ConeKotlinType> {
    val parameters = argument.valueParameters
    val expectedParameters = expectedType.extractParametersForFunctionalType(expectedTypeIsExtensionFunctionType, session)

    val nullableAnyType = argument.session.builtinTypes.nullableAnyType.type
    if (parameters.isEmpty()) {
        return expectedParameters.map { it?.type ?: nullableAnyType }
    }

    return parameters.mapIndexed { index, parameter ->
        parameter.returnTypeRef.coneTypeSafe() ?: expectedParameters.getOrNull(index) ?: nullableAnyType
    }
}

private fun ConeKotlinType.extractParametersForFunctionalType(
    isExtensionFunctionType: Boolean,
    session: FirSession
): List<ConeKotlinType?> {
    return valueParameterTypesIncludingReceiver(session).let {
        if (isExtensionFunctionType) {
            it.drop(1)
        } else {
            it
        }
    }
}

class ConeTypeVariableForLambdaReturnType(val argument: FirAnonymousFunction, name: String) : ConeTypeVariable(name)

sealed class PostponedResolvedAtom : PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<ConeKotlinType>
    abstract override val outputType: ConeKotlinType?
    override var analyzed: Boolean = false
    abstract val expectedType: ConeKotlinType?
}

class ResolvedLambdaAtom(
    val atom: FirAnonymousFunction,
    override val expectedType: ConeKotlinType?,
    val isSuspend: Boolean,
    val receiver: ConeKotlinType?,
    val parameters: List<ConeKotlinType>,
    val returnType: ConeKotlinType,
    val typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType?,
    val candidateForOuterCall: Candidate?
) : PostponedResolvedAtom() {
    init {
        candidateForOuterCall?.let {
            it.postponedAtoms += this
        }
    }

    lateinit var returnStatements: Collection<FirStatement>

    override val inputTypes: Collection<ConeKotlinType> get() = receiver?.let { parameters + it } ?: parameters
    override val outputType: ConeKotlinType get() = returnType
}

class LambdaWithTypeVariableAsExpectedTypeAtom(
    val atom: FirAnonymousFunction,
    override val expectedType: ConeKotlinType,
    val expectedTypeRef: FirTypeRef,
    val candidateForOuterCall: Candidate
) : PostponedResolvedAtom() {
    init {
        candidateForOuterCall.postponedAtoms += this
    }

    override val inputTypes: Collection<ConeKotlinType> get() = listOf(expectedType)
    override val outputType: ConeKotlinType? get() = null
}

class ResolvedCallableReferenceAtom(
    val reference: FirCallableReferenceAccess,
    override val expectedType: ConeKotlinType?,
    val lhs: DoubleColonLHS?,
    private val session: FirSession
) : PostponedResolvedAtom() {
    var postponed: Boolean = false

    var resultingCandidate: Pair<Candidate, CandidateApplicability>? = null

    override val inputTypes: Collection<ConeKotlinType>
        get() {
            if (!postponed) return emptyList()
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.inputTypes
                ?: listOfNotNull(expectedType)
        }
    override val outputType: ConeKotlinType?
        get() {
            if (!postponed) return null
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.outputType
        }
}

private data class InputOutputTypes(val inputTypes: List<ConeKotlinType>, val outputType: ConeKotlinType)

private fun extractInputOutputTypesFromCallableReferenceExpectedType(
    expectedType: ConeKotlinType?,
    session: FirSession
): InputOutputTypes? {
    if (expectedType == null) return null

    return when {
        expectedType.isBuiltinFunctionalType(session) || expectedType.isSuspendFunctionType(session) ->
            extractInputOutputTypesFromFunctionType(expectedType, session)

//        ReflectionTypes.isBaseTypeForNumberedReferenceTypes(expectedType) ->
//            InputOutputTypes(emptyList(), expectedType.arguments.single().type.unwrap())
//
//        ReflectionTypes.isNumberedKFunction(expectedType) -> {
//            val functionFromSupertype = expectedType.immediateSupertypes().first { it.isFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(functionFromSupertype)
//        }
//
//        ReflectionTypes.isNumberedKSuspendFunction(expectedType) -> {
//            val kSuspendFunctionType = expectedType.immediateSupertypes().first { it.isSuspendFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(kSuspendFunctionType)
//        }
//
//        ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(expectedType) -> {
//            val functionFromSupertype = expectedType.supertypes().first { it.isFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(functionFromSupertype)
//        }

        else -> null
    }
}

private fun extractInputOutputTypesFromFunctionType(
    functionType: ConeKotlinType,
    session: FirSession
): InputOutputTypes {
    val parameters = functionType.valueParameterTypesIncludingReceiver(session).map {
        it ?: ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(StandardClassIds.Nothing), emptyArray(),
            isNullable = false
        )
    }

    val outputType = functionType.returnType(session) ?: ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(StandardClassIds.Any), emptyArray(),
        isNullable = true
    )

    return InputOutputTypes(parameters, outputType)
}
