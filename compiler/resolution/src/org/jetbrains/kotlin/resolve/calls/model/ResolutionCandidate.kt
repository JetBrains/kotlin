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
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceResolver
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.components.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.UnwrappedType


abstract class ResolutionPart {
    abstract fun KotlinResolutionCandidate.process(workIndex: Int)

    open fun KotlinResolutionCandidate.workCount(): Int = 1

    // helper functions
    protected inline val KotlinResolutionCandidate.candidateDescriptor get() = resolvedCall.candidateDescriptor
    protected inline val KotlinResolutionCandidate.kotlinCall get() = resolvedCall.atom
}

interface KotlinDiagnosticsHolder {
    fun addDiagnostic(diagnostic: KotlinCallDiagnostic)

    class SimpleHolder : KotlinDiagnosticsHolder {
        private val diagnostics = arrayListOf<KotlinCallDiagnostic>()

        override fun addDiagnostic(diagnostic: KotlinCallDiagnostic) {
            diagnostics.add(diagnostic)
        }

        fun getDiagnostics(): List<KotlinCallDiagnostic> = diagnostics
    }
}

fun KotlinDiagnosticsHolder.addDiagnosticIfNotNull(diagnostic: KotlinCallDiagnostic?) {
    diagnostic?.let { addDiagnostic(it) }
}

/**
 * baseSystem contains all information from arguments, i.e. it is union of all system of arguments
 * Also by convention we suppose that baseSystem has no contradiction
 */
class KotlinResolutionCandidate(
    val callComponents: KotlinCallComponents,
    val resolutionCallbacks: KotlinResolutionCallbacks,
    val callableReferenceResolver: CallableReferenceResolver,
    val scopeTower: ImplicitScopeTower,
    private val baseSystem: ConstraintStorage,
    val resolvedCall: MutableResolvedCallAtom,
    val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null,
    private val resolutionSequence: List<ResolutionPart> = resolvedCall.atom.callKind.resolutionSequence
) : Candidate, KotlinDiagnosticsHolder {
    val diagnosticsFromResolutionParts = arrayListOf<KotlinCallDiagnostic>() // TODO: this is mutable list, take diagnostics only once!
    private var newSystem: NewConstraintSystemImpl? = null
    private var currentApplicability = ResolutionCandidateApplicability.RESOLVED
    private var subResolvedAtoms: MutableList<ResolvedAtom> = arrayListOf()

    private val stepCount = resolutionSequence.sumBy { it.run { workCount() } }
    private var step = 0

    fun getSystem(): NewConstraintSystem {
        if (newSystem == null) {
            newSystem = NewConstraintSystemImpl(callComponents.constraintInjector, callComponents.builtIns)
            newSystem!!.addOtherSystem(baseSystem)
        }
        return newSystem!!
    }

    internal val csBuilder get() = getSystem().getBuilder()

    override fun addDiagnostic(diagnostic: KotlinCallDiagnostic) {
        diagnosticsFromResolutionParts.add(diagnostic)
        currentApplicability = maxOf(diagnostic.candidateApplicability, currentApplicability)
    }

    fun getSubResolvedAtoms(): List<ResolvedAtom> = subResolvedAtoms

    fun addResolvedKtPrimitive(resolvedAtom: ResolvedAtom) {
        subResolvedAtoms.add(resolvedAtom)
    }

    private fun processParts(stopOnFirstError: Boolean) {
        if (stopOnFirstError && step > 0) return // error already happened
        if (step == stepCount) return

        var partIndex = 0
        var workStep = step
        while (workStep > 0) {
            val workCount = resolutionSequence[partIndex].run { workCount() }
            if (workStep >= workCount) {
                partIndex++
                workStep -= workCount
            } else {
                break
            }
        }
        if (partIndex < resolutionSequence.size) {
            if (processPart(resolutionSequence[partIndex], stopOnFirstError, workStep)) return
            partIndex++
        }

        while (partIndex < resolutionSequence.size) {
            if (processPart(resolutionSequence[partIndex], stopOnFirstError)) return
            partIndex++
        }
        if (step == stepCount) {
            resolvedCall.setAnalyzedResults(subResolvedAtoms)
        }
    }

    // true if part was interrupted
    private fun processPart(part: ResolutionPart, stopOnFirstError: Boolean, startWorkIndex: Int = 0): Boolean {
        for (workIndex in startWorkIndex until (part.run { workCount() })) {
            if (stopOnFirstError && !currentApplicability.isSuccess) return true

            part.run { process(workIndex) }
            step++
        }
        return false
    }

    val variableCandidateIfInvoke: KotlinResolutionCandidate?
        get() = callComponents.statelessCallbacks.getVariableCandidateIfInvoke(resolvedCall.atom)

    private val variableApplicability
        get() = variableCandidateIfInvoke?.resultingApplicability ?: ResolutionCandidateApplicability.RESOLVED

    override val isSuccessful: Boolean
        get() {
            processParts(stopOnFirstError = true)
            return currentApplicability.isSuccess && variableApplicability.isSuccess && !getSystem().hasContradiction
        }

    override val resultingApplicability: ResolutionCandidateApplicability
        get() {
            processParts(stopOnFirstError = false)

            val systemApplicability = getResultApplicability(getSystem().diagnostics)
            return maxOf(currentApplicability, systemApplicability, variableApplicability)
        }

    override fun addCompatibilityWarning(other: Candidate) {
        if (this !== other && other is KotlinResolutionCandidate) {
            addDiagnostic(CompatibilityWarning(other.resolvedCall.candidateDescriptor))
        }
    }

    override fun toString(): String {
        val descriptor = DescriptorRenderer.COMPACT.render(resolvedCall.candidateDescriptor)
        val okOrFail = if (currentApplicability.isSuccess) "OK" else "FAIL"
        val step = "$step/$stepCount"
        return "$okOrFail($step): $descriptor"
    }
}

class MutableResolvedCallAtom(
    override val atom: KotlinCall,
    override val candidateDescriptor: CallableDescriptor, // original candidate descriptor
    override val explicitReceiverKind: ExplicitReceiverKind,
    override val dispatchReceiverArgument: SimpleKotlinCallArgument?,
    override val extensionReceiverArgument: SimpleKotlinCallArgument?
) : ResolvedCallAtom() {
    override lateinit var typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping
    override lateinit var argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    override lateinit var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor
    override lateinit var knownParametersSubstitutor: TypeSubstitutor
    lateinit var argumentToCandidateParameter: Map<KotlinCallArgument, ValueParameterDescriptor>
    private var samAdapterMap: HashMap<KotlinCallArgument, SamConversionDescription>? = null
    private var suspendAdapterMap: HashMap<KotlinCallArgument, UnwrappedType>? = null
    private var unitAdapterMap: HashMap<KotlinCallArgument, UnwrappedType>? = null
    private var signedUnsignedConstantConversions: HashMap<KotlinCallArgument, IntegerValueTypeConstant>? = null

    val hasSamConversion: Boolean
        get() = samAdapterMap != null

    val hasSuspendConversion: Boolean
        get() = suspendAdapterMap != null

    override val argumentsWithConversion: Map<KotlinCallArgument, SamConversionDescription>
        get() = samAdapterMap ?: emptyMap()

    override val argumentsWithSuspendConversion: Map<KotlinCallArgument, UnwrappedType>
        get() = suspendAdapterMap ?: emptyMap()

    override val argumentsWithUnitConversion: Map<KotlinCallArgument, UnwrappedType>
        get() = unitAdapterMap ?: emptyMap()

    override val argumentsWithConstantConversion: Map<KotlinCallArgument, IntegerValueTypeConstant>
        get() = signedUnsignedConstantConversions ?: emptyMap()

    fun registerArgumentWithSamConversion(argument: KotlinCallArgument, samConversionDescription: SamConversionDescription) {
        if (samAdapterMap == null)
            samAdapterMap = hashMapOf()

        samAdapterMap!![argument] = samConversionDescription
    }

    fun registerArgumentWithSuspendConversion(argument: KotlinCallArgument, convertedType: UnwrappedType) {
        if (suspendAdapterMap == null)
            suspendAdapterMap = hashMapOf()

        suspendAdapterMap!![argument] = convertedType
    }

    fun registerArgumentWithUnitConversion(argument: KotlinCallArgument, convertedType: UnwrappedType) {
        if (unitAdapterMap == null)
            unitAdapterMap = hashMapOf()

        unitAdapterMap!![argument] = convertedType
    }

    fun registerArgumentWithConstantConversion(argument: KotlinCallArgument, convertedConstant: IntegerValueTypeConstant) {
        if (signedUnsignedConstantConversions == null)
            signedUnsignedConstantConversions = hashMapOf()

        signedUnsignedConstantConversions!![argument] = convertedConstant
    }


    override public fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        super.setAnalyzedResults(subResolvedAtoms)
    }

    override fun toString(): String = "$atom, candidate = $candidateDescriptor"
}

