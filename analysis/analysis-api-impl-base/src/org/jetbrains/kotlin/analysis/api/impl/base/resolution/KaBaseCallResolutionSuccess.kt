/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionSuccess
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleOrMultiCall

@KaImplementationDetail
class KaBaseCallResolutionSuccess(
    private val backingCall: KaSingleOrMultiCall,
) : KaCallResolutionSuccess {
    override val token: KaLifetimeToken get() = backingCall.token
    override val call: KaSingleOrMultiCall get() = withValidityAssertion { backingCall }
}
