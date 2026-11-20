/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.resolution

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo

internal class KaFirSuccessCallInfo(
    private val backingCall: KaCall,
) : KaSuccessCallInfo {
    override val token: KaLifetimeToken get() = backingCall.token
    override val call: KaCall get() = withValidityAssertion { backingCall }
}
