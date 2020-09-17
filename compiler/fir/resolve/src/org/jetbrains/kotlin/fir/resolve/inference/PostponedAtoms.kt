/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

//  --------------------------- Variables ---------------------------

class ConeTypeVariableForLambdaReturnType(val argument: FirAnonymousFunction, name: String) : ConeTypeVariable(name)

//  -------------------------- Atoms --------------------------

sealed class PostponedResolvedAtom : PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<ConeKotlinType>
    abstract override val outputType: ConeKotlinType?
    override var analyzed: Boolean = false
    abstract override val expectedType: ConeKotlinType?
}

//  ------------- Lambdas -------------

class ResolvedLambdaAtom(
    val atom: FirAnonymousFunction,
    override val expectedType: ConeKotlinType?,
    val isSuspend: Boolean,
    val receiver: ConeKotlinType?,
    val parameters: List<ConeKotlinType>,
    val returnType: ConeKotlinType,
    val typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType?,
    candidateOfOuterCall: Candidate?
) : PostponedResolvedAtom() {
    init {
        candidateOfOuterCall?.let {
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
    val candidateOfOuterCall: Candidate
) : PostponedResolvedAtom(), PostponedAtomWithRevisableExpectedType {
    init {
        candidateOfOuterCall.postponedAtoms += this
    }

    override val inputTypes: Collection<ConeKotlinType> get() = listOf(expectedType)
    override val outputType: ConeKotlinType? get() = null
    override var revisedExpectedType: ConeKotlinType? = null
        private set

    override fun reviseExpectedType(expectedType: KotlinTypeMarker) {
        require(expectedType is ConeKotlinType)
        revisedExpectedType = expectedType
    }
}

//  ------------- References -------------

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

//  -------------------------- Utils --------------------------

internal data class InputOutputTypes(val inputTypes: List<ConeKotlinType>, val outputType: ConeKotlinType)

internal fun extractInputOutputTypesFromCallableReferenceExpectedType(
    expectedType: ConeKotlinType?,
    session: FirSession
): InputOutputTypes? {
    if (expectedType == null) return null

    return when {
        expectedType.isBuiltinFunctionalType(session) ->
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
