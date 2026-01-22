/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseSimpleFunctionCall(
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
    private val backingArgumentMapping: Map<KtExpression, KaVariableSignature<KaParameterSymbol>>,
    private val backingTypeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
) : @Suppress("DEPRECATION") org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall {
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val signature: KaFunctionSignature<KaFunctionSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.signature }

    override val dispatchReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.dispatchReceiver }

    override val extensionReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.extensionReceiver }

    @KaExperimentalApi
    override val contextArguments: List<KaReceiverValue>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.contextArguments }

    @Deprecated(
        "Check whether the call is instance of the 'KaImplicitInvokeCall' instead",
        replaceWith = ReplaceWith(
            "this is KaImplicitInvokeCall",
            "org.jetbrains.kotlin.analysis.api.resolution.KaImplicitInvokeCall"
        )
    )
    override val isImplicitInvoke: Boolean get() = withValidityAssertion { false }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>
        get() = withValidityAssertion { backingTypeArgumentsMapping }

    override val valueArgumentMapping: Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>>
        get() = withValidityAssertion { backingArgumentMapping.toValueArgumentMapping() }

    @KaExperimentalApi
    override val contextArgumentMapping: Map<KtExpression, KaVariableSignature<KaContextParameterSymbol>>
        get() = withValidityAssertion { backingArgumentMapping.toContextArgumentMapping() }
}
