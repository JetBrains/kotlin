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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.util.*


interface ResolutionPart {
    fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic>
}

sealed class KotlinResolutionCandidate : Candidate {
    abstract val kotlinCall: KotlinCall

    abstract val lastCall: SimpleKotlinResolutionCandidate
}

class VariableAsFunctionKotlinResolutionCandidate(
        override val kotlinCall: KotlinCall,
        val resolvedVariable: SimpleKotlinResolutionCandidate,
        val invokeCandidate: SimpleKotlinResolutionCandidate
) : KotlinResolutionCandidate() {
    override val isSuccessful: Boolean get() = resolvedVariable.isSuccessful && invokeCandidate.isSuccessful
    override val resultingApplicability: ResolutionCandidateApplicability
        get() = maxOf(resolvedVariable.resultingApplicability, invokeCandidate.resultingApplicability)

    override val lastCall: SimpleKotlinResolutionCandidate get() = invokeCandidate
}

sealed class AbstractSimpleKotlinResolutionCandidate(
        val constraintSystem: NewConstraintSystem,
        initialDiagnostics: Collection<KotlinCallDiagnostic> = emptyList()
) : KotlinResolutionCandidate() {
    override val isSuccessful: Boolean
        get() {
            process(stopOnFirstError = true)
            return !hasErrors
        }

    override val resultingApplicability: ResolutionCandidateApplicability
        get() {
            process(stopOnFirstError = false)
            return getResultApplicability(diagnostics + constraintSystem.diagnostics)
        }

    private val diagnostics = ArrayList<KotlinCallDiagnostic>()
    protected var step = 0
        private set

    protected var hasErrors = false
        private set

    private fun process(stopOnFirstError: Boolean) {
        while (step < resolutionSequence.size && (!stopOnFirstError || !hasErrors)) {
            addDiagnostics(resolutionSequence[step].run { lastCall.process() })
            step++
        }
    }

    private fun addDiagnostics(diagnostics: Collection<KotlinCallDiagnostic>) {
        hasErrors = hasErrors || diagnostics.any { !it.candidateApplicability.isSuccess } ||
                    constraintSystem.diagnostics.any { !it.candidateApplicability.isSuccess }
        this.diagnostics.addAll(diagnostics)
    }

    init {
        addDiagnostics(initialDiagnostics)
    }

    fun getCandidateDiagnostics(): List<KotlinCallDiagnostic> = diagnostics

    abstract val resolutionSequence: List<ResolutionPart>
}

open class SimpleKotlinResolutionCandidate(
        val callComponents: KotlinCallComponents,
        val scopeTower: ImplicitScopeTower,
        override val kotlinCall: KotlinCall,
        val explicitReceiverKind: ExplicitReceiverKind,
        val dispatchReceiverArgument: SimpleKotlinCallArgument?,
        val extensionReceiver: SimpleKotlinCallArgument?,
        val candidateDescriptor: CallableDescriptor,
        val knownTypeParametersResultingSubstitutor: TypeSubstitutor?,
        initialDiagnostics: Collection<KotlinCallDiagnostic>
) : AbstractSimpleKotlinResolutionCandidate(NewConstraintSystemImpl(callComponents.constraintInjector, callComponents.resultTypeResolver), initialDiagnostics) {
    val csBuilder: ConstraintSystemBuilder get() = constraintSystem.getBuilder()

    lateinit var typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping
    lateinit var argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    lateinit var descriptorWithFreshTypes: CallableDescriptor
    lateinit var typeVariablesForFreshTypeParameters: List<NewTypeVariable>

    override val lastCall: SimpleKotlinResolutionCandidate get() = this
    override val resolutionSequence: List<ResolutionPart> get() = kotlinCall.callKind.resolutionSequence

    override fun toString(): String {
        val descriptor = DescriptorRenderer.COMPACT.render(candidateDescriptor)
        val okOrFail = if (hasErrors) "FAIL" else "OK"
        val step = "$step/${resolutionSequence.size}"
        return "$okOrFail($step): $descriptor"
    }
}

class ErrorKotlinResolutionCandidate(
        callComponents: KotlinCallComponents,
        scopeTower: ImplicitScopeTower,
        kotlinCall: KotlinCall,
        explicitReceiverKind: ExplicitReceiverKind,
        dispatchReceiverArgument: SimpleKotlinCallArgument?,
        extensionReceiver: SimpleKotlinCallArgument?,
        candidateDescriptor: CallableDescriptor
) : SimpleKotlinResolutionCandidate(callComponents, scopeTower, kotlinCall, explicitReceiverKind, dispatchReceiverArgument,
                                    extensionReceiver, candidateDescriptor, null, listOf()) {
    override val resolutionSequence: List<ResolutionPart> get() = emptyList()

    init {
        typeArgumentMappingByOriginal = TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
        argumentMappingByOriginal = emptyMap()
        descriptorWithFreshTypes = candidateDescriptor
    }
}
