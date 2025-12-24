/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedVariableSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
class KaBaseSimpleVariableAccessCall(
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
    private val backingTypeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    private val backingKind: KaVariableAccessCall.Kind,
    private val backingIsContextSensitive: Boolean,
) : KaSimpleVariableAccessCall {
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val signature: KaVariableSignature<KaVariableSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.signature }

    override val dispatchReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.dispatchReceiver }

    override val extensionReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.extensionReceiver }

    @KaExperimentalApi
    override val contextArguments: List<KaReceiverValue>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.contextArguments }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> get() = withValidityAssertion { backingTypeArgumentsMapping }

    override val kind: KaVariableAccessCall.Kind
        get() = withValidityAssertion { backingKind }

    @Suppress("DEPRECATION")
    @Deprecated("Use 'kind' instead", replaceWith = ReplaceWith("kind"))
    override val simpleAccess: org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccess
        get() = withValidityAssertion { backingKind as org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccess }

    override val isContextSensitive: Boolean get() = withValidityAssertion { backingIsContextSensitive }
}
