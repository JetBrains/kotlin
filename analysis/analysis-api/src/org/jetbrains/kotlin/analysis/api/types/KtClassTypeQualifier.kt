/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.name.Name

public sealed interface KtClassTypeQualifier : KtLifetimeOwner {
    public val name: Name
    public val typeArguments: List<KtTypeProjection>

    public class KtResolvedClassTypeQualifier(
        private val backingSymbol: KtClassifierSymbol,
        typeArguments: List<KtTypeProjection>,
        override val token: KtLifetimeToken
    ) : KtClassTypeQualifier {
        override val name: Name get() = withValidityAssertion { backingSymbol.nameOrAnonymous }
        public val symbol: KtClassifierSymbol get() = withValidityAssertion { backingSymbol }
        override val typeArguments: List<KtTypeProjection> by validityAsserted(typeArguments)
    }

    public class KtUnresolvedClassTypeQualifier(
        name: Name,
        typeArguments: List<KtTypeProjection>,
        override val token: KtLifetimeToken
    ) : KtClassTypeQualifier {
        override val name: Name by validityAsserted(name)
        override val typeArguments: List<KtTypeProjection> by validityAsserted(typeArguments)
    }
}

