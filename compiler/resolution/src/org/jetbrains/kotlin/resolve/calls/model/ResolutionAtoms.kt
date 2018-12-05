/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.UnwrappedType

/**
 * Call, Callable reference, lambda & function expression, collection literal.
 * In future we should add literals here, because they have similar lifecycle.
 *
 * Expression with type is also primitive. This is done for simplification. todo
 */
interface ResolutionAtom

sealed class ResolvedAtom {
    abstract val atom: ResolutionAtom? // CallResolutionResult has no ResolutionAtom

    var analyzed: Boolean = false
        private set

    lateinit var subResolvedAtoms: List<ResolvedAtom>
        private set

    protected open fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        assert(!analyzed) {
            "Already analyzed: $this"
        }

        analyzed = true

        this.subResolvedAtoms = subResolvedAtoms
    }

    // For AllCandidates mode to avoid analyzing postponed arguments
    fun setEmptyAnalyzedResults() {
        setAnalyzedResults(emptyList())
    }
}

abstract class ResolvedCallAtom : ResolvedAtom() {
    abstract override val atom: KotlinCall
    abstract val candidateDescriptor: CallableDescriptor
    abstract val explicitReceiverKind: ExplicitReceiverKind
    abstract val dispatchReceiverArgument: SimpleKotlinCallArgument?
    abstract val extensionReceiverArgument: SimpleKotlinCallArgument?
    abstract val typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val substitutor: FreshVariableNewTypeSubstitutor

    abstract val argumentsWithConversion: Map<KotlinCallArgument, SamConversionDescription>
}

class SamConversionDescription(
    val convertedTypeByOriginParameter: UnwrappedType,
    val convertedTypeByCandidateParameter: UnwrappedType // expected type for corresponding argument
)

class ResolvedExpressionAtom(override val atom: ExpressionKotlinCallArgument) : ResolvedAtom() {
    init {
        setAnalyzedResults(listOf())
    }
}

sealed class PostponedResolvedAtom : ResolvedAtom() {
    abstract val inputTypes: Collection<UnwrappedType>
    abstract val outputType: UnwrappedType?
}

class LambdaWithTypeVariableAsExpectedTypeAtom(
    override val atom: LambdaKotlinCallArgument,
    val expectedType: UnwrappedType
) : PostponedResolvedAtom() {
    override val inputTypes: Collection<UnwrappedType> get() = listOf(expectedType)
    override val outputType: UnwrappedType? get() = null

    fun setAnalyzed(resolvedLambdaAtom: ResolvedLambdaAtom) {
        setAnalyzedResults(listOf(resolvedLambdaAtom))
    }
}

class ResolvedLambdaAtom(
    override val atom: LambdaKotlinCallArgument,
    val isSuspend: Boolean,
    val receiver: UnwrappedType?,
    val parameters: List<UnwrappedType>,
    val returnType: UnwrappedType,
    val typeVariableForLambdaReturnType: TypeVariableForLambdaReturnType?
) : PostponedResolvedAtom() {
    lateinit var resultArguments: List<KotlinCallArgument>
        private set

    fun setAnalyzedResults(
        resultArguments: List<KotlinCallArgument>,
        subResolvedAtoms: List<ResolvedAtom>
    ) {
        this.resultArguments = resultArguments
        setAnalyzedResults(subResolvedAtoms)
    }

    override val inputTypes: Collection<UnwrappedType> get() = receiver?.let { parameters + it } ?: parameters
    override val outputType: UnwrappedType get() = returnType
}

class ResolvedCallableReferenceAtom(
    override val atom: CallableReferenceKotlinCallArgument,
    val expectedType: UnwrappedType?
) : PostponedResolvedAtom() {
    var candidate: CallableReferenceCandidate? = null
        private set

    fun setAnalyzedResults(
        candidate: CallableReferenceCandidate?,
        subResolvedAtoms: List<ResolvedAtom>
    ) {
        this.candidate = candidate
        setAnalyzedResults(subResolvedAtoms)
    }

    override val inputTypes: Collection<UnwrappedType>
        get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.inputTypes ?: listOfNotNull(expectedType)

    override val outputType: UnwrappedType?
        get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.outputType
}

class ResolvedCollectionLiteralAtom(
    override val atom: CollectionLiteralKotlinCallArgument,
    val expectedType: UnwrappedType?
) : ResolvedAtom() {
    init {
        setAnalyzedResults(listOf())
    }
}

sealed class CallResolutionResult(
    resultCallAtom: ResolvedCallAtom?,
    val diagnostics: List<KotlinCallDiagnostic>,
    val constraintSystem: ConstraintStorage
) : ResolvedAtom() {
    init {
        setAnalyzedResults(listOfNotNull(resultCallAtom))
    }

    final override fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        super.setAnalyzedResults(subResolvedAtoms)
    }

    override val atom: ResolutionAtom? get() = null

    override fun toString() = "diagnostics: (${diagnostics.joinToString()})"
}

open class SingleCallResolutionResult(
    val resultCallAtom: ResolvedCallAtom,
    diagnostics: List<KotlinCallDiagnostic>,
    constraintSystem: ConstraintStorage
) : CallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

class PartialCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<KotlinCallDiagnostic>,
    constraintSystem: ConstraintStorage
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

class CompletedCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<KotlinCallDiagnostic>,
    constraintSystem: ConstraintStorage
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

class ErrorCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<KotlinCallDiagnostic>,
    constraintSystem: ConstraintStorage
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

class AllCandidatesResolutionResult(
    val allCandidates: Collection<KotlinResolutionCandidate>
) : CallResolutionResult(null, emptyList(), ConstraintStorage.Empty)

fun CallResolutionResult.resultCallAtom(): ResolvedCallAtom? =
    if (this is SingleCallResolutionResult) resultCallAtom else null

val ResolvedCallAtom.freshReturnType: UnwrappedType?
    get() {
        val returnType = candidateDescriptor.returnType ?: return null
        return substitutor.safeSubstitute(returnType.unwrap())
    }