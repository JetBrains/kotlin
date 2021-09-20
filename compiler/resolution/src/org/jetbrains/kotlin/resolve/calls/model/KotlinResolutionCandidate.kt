/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.tower.Candidate
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.types.TypeSubstitutor

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
    private var currentApplicability = CandidateApplicability.RESOLVED
    private var subResolvedAtoms: MutableList<ResolvedAtom> = arrayListOf()

    private val stepCount = resolutionSequence.sumOf { it.run { workCount() } }
    private var step = 0

    fun getSystem(): NewConstraintSystem {
        if (newSystem == null) {
            newSystem =
                NewConstraintSystemImpl(callComponents.constraintInjector, callComponents.builtIns, callComponents.kotlinTypeRefiner)
            newSystem!!.addOtherSystem(baseSystem)
        }
        return newSystem!!
    }

    internal val csBuilder get() = getSystem().getBuilder()

    override fun addDiagnostic(diagnostic: KotlinCallDiagnostic) {
        diagnosticsFromResolutionParts.add(diagnostic)
        currentApplicability = minOf(diagnostic.candidateApplicability, currentApplicability)
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
        get() = variableCandidateIfInvoke?.resultingApplicability ?: CandidateApplicability.RESOLVED

    override val isSuccessful: Boolean
        get() {
            processParts(stopOnFirstError = true)
            return currentApplicability.isSuccess && variableApplicability.isSuccess && !getSystem().hasContradiction
        }

    override val resultingApplicability: CandidateApplicability
        get() {
            processParts(stopOnFirstError = false)

            val systemApplicability = getResultApplicability(getSystem().errors)
            return minOf(currentApplicability, systemApplicability, variableApplicability)
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