/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

abstract class AbstractCandidate {
    abstract val symbol: FirBasedSymbol<*>
    abstract val applicability: CandidateApplicability
}

abstract class AbstractCallCandidate<P : AbstractConeResolutionAtom> : AbstractCandidate() {
    abstract val argumentMapping: LinkedHashMap<P, FirValueParameter>
    abstract val argumentMappingInitialized: Boolean
    abstract val dispatchReceiver: AbstractConeResolutionAtom?
    abstract val chosenExtensionReceiver: AbstractConeResolutionAtom?
    abstract val explicitReceiverKind: ExplicitReceiverKind
    abstract val contextArguments: List<AbstractConeResolutionAtom>?
    abstract val callInfo: AbstractCallInfo
    abstract val diagnostics: List<ResolutionDiagnostic>
    abstract val errors: List<ConstraintSystemError>
    abstract val system: NewConstraintSystem
}
