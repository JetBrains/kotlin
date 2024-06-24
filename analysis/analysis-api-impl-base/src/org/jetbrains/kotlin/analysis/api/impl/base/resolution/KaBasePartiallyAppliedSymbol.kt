/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol

@KaImplementationDetail
class KaBasePartiallyAppliedSymbol<out S : KaCallableSymbol, out C : KaCallableSignature<S>>(
    private val backingSignature: C,
    dispatchReceiver: KaReceiverValue?,
    extensionReceiver: KaReceiverValue?,
) : KaPartiallyAppliedSymbol<S, C> {
    override val token: KaLifetimeToken get() = backingSignature.token
    override val signature: C get() = withValidityAssertion { backingSignature }
    override val dispatchReceiver: KaReceiverValue? by validityAsserted(dispatchReceiver)
    override val extensionReceiver: KaReceiverValue? by validityAsserted(extensionReceiver)
}
