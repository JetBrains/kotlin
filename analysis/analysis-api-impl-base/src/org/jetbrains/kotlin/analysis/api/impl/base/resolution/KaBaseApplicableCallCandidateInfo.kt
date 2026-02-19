/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaApplicableCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaCall

@KaImplementationDetail
class KaBaseApplicableCallCandidateInfo(
    private val backingCandidate: KaCall,
    isInBestCandidates: Boolean,
) : KaApplicableCallCandidateInfo {
    private val backingIsInBestCandidates: Boolean = isInBestCandidates
    override val token: KaLifetimeToken get() = backingCandidate.token

    override val candidate: KaCall get() = withValidityAssertion { backingCandidate }
    override val isInBestCandidates: Boolean get() = withValidityAssertion { backingIsInBestCandidates }
}
