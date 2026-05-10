/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableReferenceCall
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaExperimentalApi
@KaImplementationDetail
class KaBaseCallableReferenceCall<S : KaCallableSymbol, C : KaCallableSignature<S>>(
    internal val backingPartiallyAppliedSymbol: KaPartiallyAppliedSymbol<S, C>,
    private val backingTypeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
) : KaCallableReferenceCall<S, C> {
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    override val signature: C
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.signature }

    override val dispatchReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.dispatchReceiver }

    override val extensionReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.extensionReceiver }

    @KaExperimentalApi
    override val contextArguments: List<KaReceiverValue>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.contextArguments }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>
        get() = withValidityAssertion { backingTypeArgumentsMapping }
}
