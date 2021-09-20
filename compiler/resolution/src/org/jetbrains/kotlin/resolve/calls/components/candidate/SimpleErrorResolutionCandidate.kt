/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components.candidate

import org.jetbrains.kotlin.resolve.calls.components.ErrorDescriptorResolutionPart
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallComponents
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCallAtom
import org.jetbrains.kotlin.resolve.calls.model.ResolutionPart
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower

class SimpleErrorResolutionCandidate(
    callComponents: KotlinCallComponents,
    resolutionCallbacks: KotlinResolutionCallbacks,
    scopeTower: ImplicitScopeTower,
    baseSystem: ConstraintStorage,
    resolvedCall: MutableResolvedCallAtom
) : SimpleResolutionCandidate(callComponents, resolutionCallbacks, scopeTower, baseSystem, resolvedCall) {
    override val resolutionSequence: List<ResolutionPart> = listOf(ErrorDescriptorResolutionPart)
}