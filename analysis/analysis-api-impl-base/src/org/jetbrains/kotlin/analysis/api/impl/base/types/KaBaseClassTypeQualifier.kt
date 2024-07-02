/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.types

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaUnresolvedClassTypeQualifier
import org.jetbrains.kotlin.name.Name

@KaImplementationDetail
class KaBaseResolvedClassTypeQualifier(
    private val backingSymbol: KaClassifierSymbol,
    typeArguments: List<KaTypeProjection>,
) : KaResolvedClassTypeQualifier {
    override val token: KaLifetimeToken get() = backingSymbol.token

    override val name: Name get() = withValidityAssertion { backingSymbol.nameOrAnonymous }

    override val symbol: KaClassifierSymbol get() = withValidityAssertion { backingSymbol }

    override val typeArguments: List<KaTypeProjection> by validityAsserted(typeArguments)
}

@KaImplementationDetail
class KaBaseUnresolvedClassTypeQualifier(
    name: Name,
    typeArguments: List<KaTypeProjection>,
    override val token: KaLifetimeToken
) : KaUnresolvedClassTypeQualifier {
    override val name: Name by validityAsserted(name)

    override val typeArguments: List<KaTypeProjection> by validityAsserted(typeArguments)
}
