/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaErrorCallInfo

@KaImplementationDetail
class KaBaseErrorCallInfo(
    candidateCalls: List<KaCall>,
    private val backingDiagnostic: KaDiagnostic,
) : KaErrorCallInfo {
    override val token: KaLifetimeToken get() = backingDiagnostic.token
    override val candidateCalls: List<KaCall> by validityAsserted(candidateCalls)
    override val diagnostic: KaDiagnostic get() = withValidityAssertion { backingDiagnostic }
}
