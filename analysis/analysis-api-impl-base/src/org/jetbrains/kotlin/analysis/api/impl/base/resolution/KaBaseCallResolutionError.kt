/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionError

@KaImplementationDetail
class KaBaseCallResolutionError(
    private val backedDiagnostic: KaDiagnostic,
    private val backingCandidateCalls: List<KaCall>,
) : KaCallResolutionError {
    override val token: KaLifetimeToken get() = backedDiagnostic.token
    override val diagnostic: KaDiagnostic get() = withValidityAssertion { backedDiagnostic }
    override val candidateCalls: List<KaCall> get() = withValidityAssertion { backingCandidateCalls }
}
