/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaAnnotationCall
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaReceiverValue
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseAnnotationCall(
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
    private val backingArgumentMapping: Map<KtExpression, KaVariableSignature<KaParameterSymbol>>,
) : KaAnnotationCall {
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    @Deprecated("Use the content of the `partiallyAppliedSymbol` directly instead")
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val signature: KaFunctionSignature<KaConstructorSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.signature }

    override val dispatchReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.dispatchReceiver }

    override val extensionReceiver: KaReceiverValue?
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.extensionReceiver }

    @KaExperimentalApi
    override val contextArguments: List<KaReceiverValue>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol.contextArguments }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> get() = withValidityAssertion { emptyMap() }
    override val valueArgumentMapping: Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>>
        get() = withValidityAssertion { backingArgumentMapping.toValueArgumentMapping() }

    @KaExperimentalApi
    override val contextArgumentMapping: Map<KtExpression, KaVariableSignature<KaContextParameterSymbol>>
        get() = withValidityAssertion { backingArgumentMapping.toContextArgumentMapping() }

    override val combinedArgumentMapping: Map<KtExpression, KaVariableSignature<KaParameterSymbol>>
        get() = withValidityAssertion { backingArgumentMapping }
}