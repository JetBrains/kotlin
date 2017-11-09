/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    lateinit var diagnostics: Collection<KotlinCallDiagnostic>
        private set

    protected open fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>, diagnostics: Collection<KotlinCallDiagnostic>) {
        assert(!analyzed) {
            "Already analyzed: $this"
        }

        analyzed = true

        this.subResolvedAtoms = subResolvedAtoms
        this.diagnostics = diagnostics
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
        setAnalyzedResults(listOf(), listOf())
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
        setAnalyzedResults(listOf(resolvedLambdaAtom), listOf())
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
            subResolvedAtoms: List<ResolvedAtom>,
            diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        this.resultArguments = resultArguments
        setAnalyzedResults(subResolvedAtoms, diagnostics)
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
            subResolvedAtoms: List<ResolvedAtom>,
            diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        this.candidate = candidate
        setAnalyzedResults(subResolvedAtoms, diagnostics)
    }

    override val inputTypes: Collection<UnwrappedType>
        get() {
            val functionType = getFunctionTypeFromCallableReferenceExpectedType(expectedType) ?: return emptyList()
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
        setAnalyzedResults(listOf(), listOf())
    }
}

class CallResolutionResult(
        val type: Type,
        val resultCallAtom: ResolvedCallAtom?,
        diagnostics: List<KotlinCallDiagnostic>,
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
        setAnalyzedResults(listOfNotNull(resultCallAtom), diagnostics)
    }

    override fun toString() = "$type, resultCallAtom = $resultCallAtom, (${diagnostics.joinToString()})"
}

val ResolvedCallAtom.freshReturnType: UnwrappedType? get() {
    val returnType = candidateDescriptor.returnType ?: return null
    return substitutor.safeSubstitute(returnType.unwrap())
}