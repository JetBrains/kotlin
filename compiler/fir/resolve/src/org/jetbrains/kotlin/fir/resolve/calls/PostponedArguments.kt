/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirAnonymousFunction,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef,
    acceptLambdaAtoms: (PostponedResolvedAtomMarker) -> Unit,
    forceResolution: Boolean = false
) {
    if (expectedType != null && !forceResolution && csBuilder.isTypeVariable(expectedType)) {
        //return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType)
    }

    val resolvedArgument =
        extractLambdaInfoFromFunctionalType(expectedType, expectedTypeRef, argument)
            ?: extraLambdaInfo(expectedType, expectedTypeRef, argument, csBuilder)

    if (expectedType != null) {
//        val lambdaType = createFunctionType(
//            csBuilder.builtIns, Annotations.EMPTY, resolvedArgument.receiver,
//            resolvedArgument.parameters, null, resolvedArgument.returnType, resolvedArgument.isSuspend
//        )
//        csBuilder.addSubtypeConstraint(lambdaType, expectedType, ArgumentConstraintPosition(argument))
    }

    acceptLambdaAtoms(resolvedArgument)
}


private val ConeKotlinType.isBuiltinFunctionalType: Boolean
    get () {
        val type = this
        return when (type) {
            is ConeClassType -> type.lookupTag.classId.asString().startsWith("kotlin/Function")
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
    if (isFunctionalTypeWithReceiver(expectedTypeRef)) {
        return (this.typeArguments.first() as ConeTypedProjection).type
    }
    return null
}

private fun isFunctionalTypeWithReceiver(typeRef: FirTypeRef) =
    typeRef.annotations.any {
        val coneTypeSafe = it.annotationTypeRef.coneTypeSafe<ConeClassType>() ?: return@any false
        coneTypeSafe.lookupTag.classId.asString() == "kotlin/ExtensionFunctionType"
    }

private val ConeKotlinType.returnType: ConeKotlinType?
    get() {
        require(this is ConeClassType)
        val projection = typeArguments.last()
        return (projection as? ConeTypedProjection)?.type
    }

private val ConeKotlinType.valueParameterTypes: List<ConeKotlinType?>
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
        expectedType != null && expectedType.isBuiltinFunctionalType//isNotNullOrNullableFunctionSupertype(expectedType)
    val argumentAsFunctionExpression = argument//.safeAs<FunctionExpression>()

    val typeVariable = TypeVariableForLambdaReturnType(argument, "_L")

    val receiverType = argumentAsFunctionExpression?.receiverType
    val returnType =
        argumentAsFunctionExpression?.returnType
            ?: expectedType?.typeArguments?.singleOrNull()?.safeAs<ConeTypedProjection>()?.type?.takeIf { isFunctionSupertype }
            ?: typeVariable.defaultType

    val nothingType = StandardClassIds.Nothing(argument.session.service()).constructType(emptyArray(), false)
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
    if (expectedType == null || !expectedType.isBuiltinFunctionalType) return null
    val parameters = extractLambdaParameters(expectedType, argument)

    val argumentAsFunctionExpression = argument//.safeAs<FunctionExpression>()
    val receiverType = argumentAsFunctionExpression.receiverType ?: expectedType.receiverType(expectedTypeRef)
    val returnType = argumentAsFunctionExpression.returnType ?: expectedType.returnType ?: return null

    return ResolvedLambdaAtom(
        argument,
        expectedType.isSuspendFunctionType,
        receiverType,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = null
    )
}

private fun extractLambdaParameters(expectedType: ConeKotlinType, argument: FirAnonymousFunction): List<ConeKotlinType> {
    val parameters = argument.valueParameters
    val expectedParameters = expectedType.valueParameterTypes

    val nullableAnyType = StandardClassIds.Any(argument.session.service()).constructType(emptyArray(), true)

    if (parameters.isEmpty()) {
        return expectedParameters.map { it?.type ?: nullableAnyType }
    }

    return parameters.mapIndexed { index, parameter ->
        parameter.returnTypeRef.coneTypeSafe() ?: expectedParameters.getOrNull(index) ?: nullableAnyType
        //expectedType.builtIns.nullableAnyType
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