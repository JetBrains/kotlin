/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.util.CancellationChecker

class CallableReferenceOverloadConflictResolver(
    builtIns: KotlinBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    statelessCallbacks: KotlinResolutionStatelessCallbacks,
    constraintInjector: ConstraintInjector,
    kotlinTypeRefiner: KotlinTypeRefiner,
) : OverloadingConflictResolver<CallableReferenceResolutionCandidate>(
    builtIns,
    module,
    specificityComparator,
    platformOverloadsSpecificityComparator,
    cancellationChecker,
    { it.candidate },
    { statelessCallbacks.createConstraintSystemForOverloadResolution(constraintInjector, builtIns) },
    Companion::createFlatSignature,
    { null },
    { statelessCallbacks.isDescriptorFromSource(it) },
    null,
    kotlinTypeRefiner,
) {
    companion object {
        private fun createFlatSignature(candidate: CallableReferenceResolutionCandidate) =
            FlatSignature.createFromReflectionType(
                candidate, candidate.candidate, candidate.numDefaults, hasBoundExtensionReceiver = candidate.extensionReceiver != null,
                candidate.reflectionCandidateType
            )
    }
}