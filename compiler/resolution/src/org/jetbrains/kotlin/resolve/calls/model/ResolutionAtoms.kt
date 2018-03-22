/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceCandidate
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.components.getFunctionTypeFromCallableReferenceExpectedType
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
}

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
        get() {
            val functionType = getFunctionTypeFromCallableReferenceExpectedType(expectedType) ?: return listOfNotNull(expectedType)
            val parameters = functionType.getValueParameterTypesFromFunctionType().map { it.type.unwrap() }
            val receiver = functionType.getReceiverTypeFromFunctionType()?.unwrap()
            return receiver?.let { parameters + it } ?: parameters
        }

    override val outputType: UnwrappedType?
        get() {
            val functionType = getFunctionTypeFromCallableReferenceExpectedType(expectedType) ?: return null
            return functionType.getReturnTypeFromFunctionType().unwrap()
        }
}

class ResolvedCollectionLiteralAtom(
    override val atom: CollectionLiteralKotlinCallArgument,
    val expectedType: UnwrappedType?
) : ResolvedAtom() {
    init {
        setAnalyzedResults(listOf())
    }
}

class CallResolutionResult(
    val type: Type,
    val resultCallAtom: ResolvedCallAtom?,
    val diagnostics: List<KotlinCallDiagnostic>,
    val constraintSystem: ConstraintStorage,
    val allCandidates: Collection<KotlinResolutionCandidate>? = null
) : ResolvedAtom() {
    override val atom: ResolutionAtom? get() = null

    enum class Type {
        COMPLETED, // resultSubstitutor possible create use constraintSystem
        PARTIAL,
        ERROR, // if resultCallAtom == null it means that there is errors NoneCandidates or ManyCandidates
        ALL_CANDIDATES // allCandidates != null
    }

    init {
        setAnalyzedResults(listOfNotNull(resultCallAtom))
    }

    override fun toString() = "$type, resultCallAtom = $resultCallAtom, (${diagnostics.joinToString()})"
}

val ResolvedCallAtom.freshReturnType: UnwrappedType?
    get() {
        val returnType = candidateDescriptor.returnType ?: return null
        return substitutor.safeSubstitute(returnType.unwrap())
    }