/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceCandidate
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.components.extractInputOutputTypesFromCallableReferenceExpectedType
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintError
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForCallableReferenceReturnType
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.typeUtil.unCapture
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

    var subResolvedAtoms: List<ResolvedAtom>? = null
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
    abstract val freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor
    abstract val knownParametersSubstitutor: TypeSubstitutor
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

interface PostponedResolvedAtomMarker {
    val inputTypes: Collection<KotlinTypeMarker>
    val outputType: KotlinTypeMarker?
    val analyzed: Boolean
}

sealed class PostponedResolvedAtom : ResolvedAtom(), PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<UnwrappedType>
    abstract override val outputType: UnwrappedType?
    abstract val expectedType: UnwrappedType?
}

class LambdaWithTypeVariableAsExpectedTypeAtom(
    override val atom: LambdaKotlinCallArgument,
    override val expectedType: UnwrappedType
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
    val typeVariableForLambdaReturnType: TypeVariableForLambdaReturnType?,
    override val expectedType: UnwrappedType?
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

abstract class ResolvedCallableReferenceAtom(
    override val atom: CallableReferenceKotlinCallArgument,
    override val expectedType: UnwrappedType?
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

}

class EagerCallableReferenceAtom(
    atom: CallableReferenceKotlinCallArgument,
    expectedType: UnwrappedType?
) : ResolvedCallableReferenceAtom(atom, expectedType) {

    override val inputTypes: Collection<UnwrappedType> get() = emptyList()
    override val outputType: UnwrappedType? get() = null

    fun transformToPostponed(): PostponedCallableReferenceAtom = PostponedCallableReferenceAtom(this)
}

sealed class AbstractPostponedCallableReferenceAtom(
    atom: CallableReferenceKotlinCallArgument,
    expectedType: UnwrappedType?
) : ResolvedCallableReferenceAtom(atom, expectedType) {
    override val inputTypes: Collection<UnwrappedType>
        get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.inputTypes ?: listOfNotNull(expectedType)

    override val outputType: UnwrappedType?
        get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.outputType
}

class CallableReferenceWithTypeVariableAsExpectedTypeAtom(
    atom: CallableReferenceKotlinCallArgument,
    expectedType: UnwrappedType?,
    val typeVariableForReturnType: TypeVariableForCallableReferenceReturnType?
) : AbstractPostponedCallableReferenceAtom(atom, expectedType)

class PostponedCallableReferenceAtom(
    eagerCallableReferenceAtom: EagerCallableReferenceAtom
) : AbstractPostponedCallableReferenceAtom(eagerCallableReferenceAtom.atom, eagerCallableReferenceAtom.expectedType)

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

    fun completedDiagnostic(substitutor: NewTypeSubstitutor): List<KotlinCallDiagnostic> {
        return diagnostics.map {
            if (it !is NewConstraintError) return@map it
            val lowerType = it.lowerType.safeAs<KotlinType>()?.unwrap() ?: return@map it
            val newLowerType = substitutor.safeSubstitute(lowerType.unCapture())
            NewConstraintError(newLowerType, it.upperType, it.position)
        }
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
    val allCandidates: Collection<CandidateWithDiagnostics>
) : CallResolutionResult(null, emptyList(), ConstraintStorage.Empty)

data class CandidateWithDiagnostics(val candidate: KotlinResolutionCandidate, val diagnostics: List<KotlinCallDiagnostic>)

fun CallResolutionResult.resultCallAtom(): ResolvedCallAtom? =
    if (this is SingleCallResolutionResult) resultCallAtom else null

val ResolvedCallAtom.freshReturnType: UnwrappedType?
    get() {
        val returnType = candidateDescriptor.returnType ?: return null
        return freshVariablesSubstitutor.safeSubstitute(returnType.unwrap())
    }

class PartialCallContainer(val result: PartialCallResolutionResult?) {
    companion object {
        val empty = PartialCallContainer(null)
    }
}

/*
 * Used only for delegated properties with one good candidate and one for bad
 * e.g. in case `var x by lazy { "" }
 */
class StubResolvedAtom(val typeVariable: TypeConstructor) : ResolvedAtom() {
    override val atom: ResolutionAtom? get() = null
}