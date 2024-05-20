/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.name.Name

public sealed interface KaClassTypeQualifier : KaLifetimeOwner {
    public val name: Name
    public val typeArguments: List<KaTypeProjection>

    public class KaResolvedClassTypeQualifier(
        private val backingSymbol: KaClassifierSymbol,
        typeArguments: List<KaTypeProjection>,
        override val token: KaLifetimeToken
    ) : KaClassTypeQualifier {
        override val name: Name get() = withValidityAssertion { backingSymbol.nameOrAnonymous }
        public val symbol: KaClassifierSymbol get() = withValidityAssertion { backingSymbol }
        override val typeArguments: List<KaTypeProjection> by validityAsserted(typeArguments)
    }

    public class KaUnresolvedClassTypeQualifier(
        name: Name,
        typeArguments: List<KaTypeProjection>,
        override val token: KaLifetimeToken
    ) : KaClassTypeQualifier {
        override val name: Name by validityAsserted(name)
        override val typeArguments: List<KaTypeProjection> by validityAsserted(typeArguments)
    }
}

public typealias KtClassTypeQualifier = KaClassTypeQualifier