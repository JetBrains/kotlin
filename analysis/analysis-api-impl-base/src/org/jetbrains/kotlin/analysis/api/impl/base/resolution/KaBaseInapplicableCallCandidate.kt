/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaInapplicableCallCandidate
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleCall

@KaImplementationDetail
class KaBaseInapplicableCallCandidate(
    private val backingCandidate: KaSingleCall<*, *>,
    private val backingIsInBestCandidates: Boolean,
    private val backingDiagnostic: KaDiagnostic,
) : KaInapplicableCallCandidate {
    override val token: KaLifetimeToken get() = backingCandidate.token

    override val candidate: KaSingleCall<*, *> get() = withValidityAssertion { backingCandidate }
    override val isInBestCandidates: Boolean get() = withValidityAssertion { backingIsInBestCandidates }
    override val diagnostic: KaDiagnostic get() = withValidityAssertion { backingDiagnostic }
}
