/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components.candidate

import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallComponents
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCallAtom
import org.jetbrains.kotlin.resolve.calls.model.ResolvedAtom
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.types.TypeSubstitutor

/**
 * baseSystem contains all information from arguments, i.e. it is union of all system of arguments
 * Also by convention we suppose that baseSystem has no contradiction
 */
open class SimpleResolutionCandidate(
    override val callComponents: KotlinCallComponents,
    override val resolutionCallbacks: KotlinResolutionCallbacks,
    override val scopeTower: ImplicitScopeTower,
    override val baseSystem: ConstraintStorage,
    override val resolvedCall: MutableResolvedCallAtom,
    override val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null,
) : ResolutionCandidate() {
    override val variableCandidateIfInvoke: ResolutionCandidate?
        get() = callComponents.statelessCallbacks.getVariableCandidateIfInvoke(resolvedCall.atom)

    override fun getSubResolvedAtoms(): List<ResolvedAtom> = subResolvedAtoms

    override fun addResolvedKtPrimitive(resolvedAtom: ResolvedAtom) {
        subResolvedAtoms.add(resolvedAtom)
    }

    private var subResolvedAtoms: MutableList<ResolvedAtom> = arrayListOf()
}
