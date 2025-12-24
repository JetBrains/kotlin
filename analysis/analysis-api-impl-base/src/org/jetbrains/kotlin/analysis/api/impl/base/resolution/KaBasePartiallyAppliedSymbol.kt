/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSingleCall
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol

@KaImplementationDetail
class KaBasePartiallyAppliedSymbol<out S : KaCallableSymbol, out C : KaCallableSignature<S>>(
    private val backingSignature: C,
    dispatchReceiver: KaReceiverValue?,
    extensionReceiver: KaReceiverValue?,
    contextArguments: List<KaReceiverValue>,
) : KaPartiallyAppliedSymbol<S, C> {
    private val backingDispatchReceiver: KaReceiverValue? = dispatchReceiver
    private val backingExtensionReceiver: KaReceiverValue? = extensionReceiver
    private val backingContextArguments: List<KaReceiverValue> = contextArguments
    override val token: KaLifetimeToken get() = backingSignature.token

    override val signature: C get() = withValidityAssertion { backingSignature }
    override val dispatchReceiver: KaReceiverValue? get() = withValidityAssertion { backingDispatchReceiver }
    override val extensionReceiver: KaReceiverValue? get() = withValidityAssertion { backingExtensionReceiver }
    override val contextArguments: List<KaReceiverValue> get() = withValidityAssertion { backingContextArguments }
}

internal val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaSingleCall<S, C>.asPartiallyAppliedSymbol: KaPartiallyAppliedSymbol<S, C>
    get() = KaBasePartiallyAppliedSymbol(signature, dispatchReceiver, extensionReceiver, contextArguments)
