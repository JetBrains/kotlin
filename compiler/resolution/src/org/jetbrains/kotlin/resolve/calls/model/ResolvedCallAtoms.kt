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

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.LowerPriorityToPreserveCompatibility
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.utils.SmartList


abstract class ResolutionPart {
    abstract fun ResolutionCandidate.process(workIndex: Int)

    open fun ResolutionCandidate.workCount(): Int = 1

    // helper functions
    protected inline val ResolutionCandidate.candidateDescriptor get() = resolvedCall.candidateDescriptor
    protected inline val ResolutionCandidate.kotlinCall get() = resolvedCall.atom
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

fun KotlinDiagnosticsHolder.addError(error: ConstraintSystemError) {
    addDiagnostic(error.asDiagnostic())
}

class ResolvedCallableReferenceCallAtom(
    atom: KotlinCall,
    candidateDescriptor: CallableDescriptor,
    explicitReceiverKind: ExplicitReceiverKind,
    dispatchReceiverArgument: SimpleKotlinCallArgument?,
    extensionReceiverArgument: SimpleKotlinCallArgument?,
    reflectionCandidateType: UnwrappedType? = null,
    candidate: CallableReferenceResolutionCandidate? = null
) : MutableResolvedCallAtom(
    atom, candidateDescriptor, explicitReceiverKind, dispatchReceiverArgument, extensionReceiverArgument, emptyList(), reflectionCandidateType, candidate
), ResolvedCallableReferenceAtom

open class MutableResolvedCallAtom(
    override val atom: KotlinCall,
    originalCandidateDescriptor: CallableDescriptor, // original candidate descriptor
    override val explicitReceiverKind: ExplicitReceiverKind,
    override val dispatchReceiverArgument: SimpleKotlinCallArgument?,
    override var extensionReceiverArgument: SimpleKotlinCallArgument?,
    override val extensionReceiverArgumentCandidates: List<SimpleKotlinCallArgument>?,
    open val reflectionCandidateType: UnwrappedType? = null,
    open val candidate: CallableReferenceResolutionCandidate? = null
) : ResolvedCallAtom() {
    override var contextReceiversArguments: List<SimpleKotlinCallArgument> = listOf()
    override lateinit var typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping
    override lateinit var argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    override lateinit var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor
    override lateinit var knownParametersSubstitutor: NewTypeSubstitutor
    lateinit var argumentToCandidateParameter: Map<KotlinCallArgument, ValueParameterDescriptor>
    private var samAdapterMap: HashMap<KotlinCallArgument, SamConversionDescription>? = null
    private var suspendAdapterMap: HashMap<KotlinCallArgument, UnwrappedType>? = null
    private var unitAdapterMap: HashMap<KotlinCallArgument, UnwrappedType>? = null
    private var signedUnsignedConstantConversions: HashMap<KotlinCallArgument, IntegerValueTypeConstant>? = null
    private var _candidateDescriptor = originalCandidateDescriptor

    override val candidateDescriptor: CallableDescriptor
        get() = _candidateDescriptor

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

    override fun setCandidateDescriptor(newCandidateDescriptor: CallableDescriptor) {
        if (newCandidateDescriptor == candidateDescriptor) return
        _candidateDescriptor = newCandidateDescriptor
    }

    override fun toString(): String = "$atom, candidate = $candidateDescriptor"
}

fun ResolutionCandidate.markCandidateForCompatibilityResolve() {
    if (callComponents.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) return
    addDiagnostic(LowerPriorityToPreserveCompatibility.asDiagnostic())
}

fun CallableReferencesCandidateFactory.markCandidateForCompatibilityResolve(diagnostics: SmartList<KotlinCallDiagnostic>) {
    if (callComponents.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) return
    diagnostics.add(LowerPriorityToPreserveCompatibility.asDiagnostic())
}
