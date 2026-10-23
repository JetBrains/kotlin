/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaUnresolvedClassTypeQualifier
import org.jetbrains.kotlin.name.Name

internal class KaFirResolvedClassTypeQualifier(
    private val backingSymbol: KaClassifierSymbol,
    typeArguments: List<KaTypeProjection>,
) : KaResolvedClassTypeQualifier {
    private val backingTypeArguments: List<KaTypeProjection> = typeArguments
    override val token: KaLifetimeToken get() = backingSymbol.token

    override val name: Name get() = withValidityAssertion { backingSymbol.nameOrAnonymous }
    override val symbol: KaClassifierSymbol get() = withValidityAssertion { backingSymbol }
    override val typeArguments: List<KaTypeProjection> get() = withValidityAssertion { backingTypeArguments }
}

internal class KaFirUnresolvedClassTypeQualifier(
    name: Name,
    typeArguments: List<KaTypeProjection>,
    override val token: KaLifetimeToken,
) : KaUnresolvedClassTypeQualifier {
    private val backingName: Name = name
    private val backingTypeArguments: List<KaTypeProjection> = typeArguments

    override val name: Name get() = withValidityAssertion { backingName }
    override val typeArguments: List<KaTypeProjection> get() = withValidityAssertion { backingTypeArguments }
}
