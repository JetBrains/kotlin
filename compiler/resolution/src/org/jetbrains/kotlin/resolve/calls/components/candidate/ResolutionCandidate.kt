/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components.candidate

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.components.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.util.ArrayList

sealed class ResolutionCandidate : Candidate, KotlinDiagnosticsHolder {
    abstract val resolvedCall: MutableResolvedCallAtom
    abstract val callComponents: KotlinCallComponents
    abstract fun getSubResolvedAtoms(): List<ResolvedAtom>
    abstract fun addResolvedKtPrimitive(resolvedAtom: ResolvedAtom)
    abstract val variableCandidateIfInvoke: ResolutionCandidate?
    abstract val scopeTower: ImplicitScopeTower
    abstract val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
    abstract val resolutionCallbacks: KotlinResolutionCallbacks
    protected abstract val baseSystem: ConstraintStorage?

    override fun addDiagnostic(diagnostic: KotlinCallDiagnostic) {
        mutableDiagnostics.add(diagnostic)
        currentApplicability = minOf(diagnostic.candidateApplicability, currentApplicability)
    }

    private val variableApplicability
        get() = variableCandidateIfInvoke?.resultingApplicability ?: CandidateApplicability.RESOLVED

    val descriptor: CallableDescriptor get() = resolvedCall.candidateDescriptor

    protected val mutableDiagnostics: ArrayList<KotlinCallDiagnostic> = arrayListOf()

    open val resolutionSequence: List<ResolutionPart> get() = resolvedCall.atom.callKind.resolutionSequence
    private var newSystem: NewConstraintSystemImpl? = null

    val diagnostics: List<KotlinCallDiagnostic> = mutableDiagnostics

    fun getSystem(): NewConstraintSystem {
        if (newSystem == null) {
            newSystem = NewConstraintSystemImpl(
                callComponents.constraintInjector, callComponents.builtIns, callComponents.kotlinTypeRefiner
            )
            if (baseSystem != null) {
                newSystem!!.addOtherSystem(baseSystem!!)
            }
        }
        return newSystem!!
    }

    private val stepCount get() = resolutionSequence.sumOf { it.run { workCount() } }
    private var step = 0

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
            resolvedCall.setAnalyzedResults(getSubResolvedAtoms())
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

    protected var currentApplicability: CandidateApplicability = CandidateApplicability.RESOLVED

    override val isSuccessful: Boolean
        get() {
            processParts(stopOnFirstError = true)
            return resultingApplicabilities.minOrNull()!!.isSuccess && !getSystem().hasContradiction
        }

    val resultingApplicabilities: Array<CandidateApplicability>
        get() = arrayOf(currentApplicability, getResultApplicability(getSystem().errors), variableApplicability)

    override val resultingApplicability: CandidateApplicability
        get() {
            processParts(stopOnFirstError = false)
            return resultingApplicabilities.minOrNull()!!
        }

    override fun addCompatibilityWarning(other: Candidate) {
        if (other is ResolutionCandidate && this !== other && this::class == other::class) {
            addDiagnostic(CompatibilityWarning(other.descriptor))
        }
    }

    override fun toString(): String {
        val descriptor = DescriptorRenderer.COMPACT.render(resolvedCall.candidateDescriptor)
        val okOrFail = if (resultingApplicabilities.minOrNull()!!.isSuccess) "OK" else "FAIL"
        val step = "$step/$stepCount"
        return "$okOrFail($step): $descriptor"
    }
}