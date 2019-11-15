/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Candidate.preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirAnonymousFunction,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef,
    forceResolution: Boolean = false
) {
    if (expectedType != null && !forceResolution && csBuilder.isTypeVariable(expectedType)) {
        //return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType)
    }

    val resolvedArgument =
        extractLambdaInfoFromFunctionalType(expectedType, expectedTypeRef, argument)
            ?: extraLambdaInfo(expectedType, expectedTypeRef, argument, csBuilder)

    if (expectedType != null) {
        // TODO: add SAM conversion processing
        val lambdaType = createFunctionalType(resolvedArgument.parameters, resolvedArgument.receiver, resolvedArgument.returnType)
        csBuilder.addSubtypeConstraint(lambdaType, expectedType, SimpleConstraintSystemConstraintPosition)
//        val lambdaType = createFunctionType(
//            csBuilder.builtIns, Annotations.EMPTY, resolvedArgument.receiver,
//            resolvedArgument.parameters, null, resolvedArgument.returnType, resolvedArgument.isSuspend
//        )
//        csBuilder.addSubtypeConstraint(lambdaType, expectedType, ArgumentConstraintPosition(argument))
    }

    postponedAtoms += resolvedArgument
}

fun Candidate.preprocessCallableReference(
    argument: FirCallableReferenceAccess,
    expectedType: ConeKotlinType
) {
    val lhs = bodyResolveComponents.doubleColonExpressionResolver.resolveDoubleColonLHS(argument)
    postponedAtoms += ResolvedCallableReferenceAtom(argument, expectedType, lhs)
}

val ConeKotlinType.isBuiltinFunctionalType: Boolean
    get() {
        return when (this) {
            is ConeClassType -> this.lookupTag.classId.asString().startsWith("kotlin/Function")
            else -> false
        }
    }


private val ConeKotlinType.isSuspendFunctionType: Boolean
    get() {
        val type = this
        return when (type) {
            is ConeClassType -> {
                val classId = type.lookupTag.classId
                classId.packageFqName.asString() == "kotlin" && classId.relativeClassName.asString().startsWith("SuspendFunction")
            }
            else -> false
        }
    }

private fun ConeKotlinType.receiverType(expectedTypeRef: FirTypeRef): ConeKotlinType? {
    if (expectedTypeRef.isExtensionFunctionType()) {
        return (this.typeArguments.first() as ConeTypedProjection).type
    }
    return null
}

val ConeKotlinType.returnType: ConeKotlinType?
    get() {
        require(this is ConeClassType)
        val projection = typeArguments.last()
        return (projection as? ConeTypedProjection)?.type
    }

val ConeKotlinType.valueParameterTypes: List<ConeKotlinType?>
    get() {
        require(this is ConeClassType)
        return typeArguments.dropLast(1).map {
            (it as? ConeTypedProjection)?.type
        }
    }

private val FirAnonymousFunction.returnType get() = returnTypeRef.coneTypeSafe<ConeKotlinType>()
private val FirAnonymousFunction.receiverType get() = receiverTypeRef?.coneTypeSafe<ConeKotlinType>()


private fun extraLambdaInfo(
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef,
    argument: FirAnonymousFunction,
    csBuilder: ConstraintSystemBuilder
): ResolvedLambdaAtom {
//    val builtIns = csBuilder.builtIns
    val isSuspend = expectedType?.isSuspendFunctionType ?: false

    val isFunctionSupertype =
        expectedType != null && expectedType.lowerBoundIfFlexible().isBuiltinFunctionalType//isNotNullOrNullableFunctionSupertype(expectedType)
    val argumentAsFunctionExpression = argument//.safeAs<FunctionExpression>()

    val typeVariable = TypeVariableForLambdaReturnType(argument, "_L")

    val receiverType = argumentAsFunctionExpression?.receiverType
    val returnType =
        argumentAsFunctionExpression?.returnType
            ?: expectedType?.typeArguments?.singleOrNull()?.safeAs<ConeTypedProjection>()?.type?.takeIf { isFunctionSupertype }
            ?: typeVariable.defaultType

    val nothingType = StandardClassIds.Nothing(argument.session.firSymbolProvider).constructType(emptyArray(), false)
    val parameters = argument.valueParameters?.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: nothingType
    } ?: emptyList()

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    return ResolvedLambdaAtom(argument, isSuspend, receiverType, parameters, returnType, typeVariable.takeIf { newTypeVariableUsed })
}

internal fun extractLambdaInfoFromFunctionalType(
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef,
    argument: FirAnonymousFunction
): ResolvedLambdaAtom? {
    if (expectedType == null) return null
    if (expectedType is ConeFlexibleType) return extractLambdaInfoFromFunctionalType(expectedType.lowerBound, expectedTypeRef, argument)
    if (!expectedType.isBuiltinFunctionalType) return null

    val argumentAsFunctionExpression = argument//.safeAs<FunctionExpression>()
    val receiverType = argumentAsFunctionExpression.receiverType ?: expectedType.receiverType(expectedTypeRef)
    val returnType = argumentAsFunctionExpression.returnType ?: expectedType.returnType ?: return null
    val parameters = extractLambdaParameters(expectedType, argument, expectedTypeRef.isExtensionFunctionType())

    return ResolvedLambdaAtom(
        argument,
        expectedType.isSuspendFunctionType,
        receiverType,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = null
    )
}

private fun extractLambdaParameters(expectedType: ConeKotlinType, argument: FirAnonymousFunction, expectedTypeIsExtensionFunctionType: Boolean): List<ConeKotlinType> {
    val parameters = argument.valueParameters
    val expectedParameters = expectedType.extractParametersForFunctionalType(expectedTypeIsExtensionFunctionType)

    val nullableAnyType = StandardClassIds.Any(argument.session.firSymbolProvider).constructType(emptyArray(), true)

    if (parameters.isEmpty()) {
        return expectedParameters.map { it?.type ?: nullableAnyType }
    }

    return parameters.mapIndexed { index, parameter ->
        parameter.returnTypeRef.coneTypeSafe() ?: expectedParameters.getOrNull(index) ?: nullableAnyType
        //expectedType.builtIns.nullableAnyType
    }
}

private fun ConeKotlinType.extractParametersForFunctionalType(isExtensionFunctionType: Boolean): List<ConeKotlinType?> {
    return valueParameterTypes.let {
        if (isExtensionFunctionType) {
            it.drop(1)
        } else {
            it
        }
    }
}

class TypeVariableForLambdaReturnType(val argument: FirAnonymousFunction, name: String) : ConeTypeVariable(name)

class ResolvedLambdaAtom(
    val atom: FirAnonymousFunction,
    val isSuspend: Boolean,
    val receiver: ConeKotlinType?,
    val parameters: List<ConeKotlinType>,
    val returnType: ConeKotlinType,
    val typeVariableForLambdaReturnType: TypeVariableForLambdaReturnType?
) : PostponedResolvedAtomMarker {

    override var analyzed: Boolean = false

//    lateinit var resultArguments: List<KotlinCallArgument>
//        private set

//    fun setAnalyzedResults(
//        resultArguments: List<KotlinCallArgument>,
//        subResolvedAtoms: List<ResolvedAtom>
//    ) {
//        this.resultArguments = resultArguments
//        setAnalyzedResults(subResolvedAtoms)
//    }

    override val inputTypes: Collection<ConeKotlinType> get() = receiver?.let { parameters + it } ?: parameters
    override val outputType: ConeKotlinType get() = returnType
}

class ResolvedCallableReferenceAtom(
    val reference: FirCallableReferenceAccess,
    val expectedType: ConeKotlinType?,
    val lhs: DoubleColonLHS?
) : PostponedResolvedAtomMarker {
    override var analyzed: Boolean = false
    var postponed: Boolean = false

    var resultingCandidate: Pair<Candidate, CandidateApplicability>? = null

    override val inputTypes: Collection<ConeKotlinType>
        get() {
            if (!postponed) return emptyList()
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.inputTypes ?: listOfNotNull(expectedType)
        }
    override val outputType: ConeKotlinType?
        get() {
            if (!postponed) return null
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.outputType
        }
}

private data class InputOutputTypes(val inputTypes: List<ConeKotlinType>, val outputType: ConeKotlinType)

private fun extractInputOutputTypesFromCallableReferenceExpectedType(expectedType: ConeKotlinType?): InputOutputTypes? {
    if (expectedType == null) return null

    return when {
        expectedType.isBuiltinFunctionalType || expectedType.isSuspendFunctionType ->
            extractInputOutputTypesFromFunctionType(expectedType)

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

private fun extractInputOutputTypesFromFunctionType(functionType: ConeKotlinType): InputOutputTypes {
    val receiver = null// TODO: functionType.receiverType()
    val parameters = functionType.valueParameterTypes.map {
        it ?: ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(StandardClassIds.Nothing), emptyArray(),
            isNullable = false
        )
    }

    val inputTypes = /*listOfNotNull(receiver) +*/ parameters
    val outputType = functionType.returnType ?: ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(StandardClassIds.Any), emptyArray(),
        isNullable = true
    )

    return InputOutputTypes(inputTypes, outputType)
}
