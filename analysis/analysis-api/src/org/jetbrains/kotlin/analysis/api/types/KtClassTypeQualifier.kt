/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.name.Name

public sealed interface KtClassTypeQualifier : KtLifetimeOwner {
    public val name: Name
    public val typeArguments: List<KtTypeProjection>

    public class KtResolvedClassTypeQualifier(
        private val _symbol: KtClassifierSymbol,
        private val _typeArguments: List<KtTypeProjection>,
        override val token: KtLifetimeToken
    ) : KtClassTypeQualifier {
        override val name: Name get() = withValidityAssertion { _symbol.nameOrAnonymous }
        public val symbol: KtClassifierSymbol get() = withValidityAssertion { _symbol }
        override val typeArguments: List<KtTypeProjection> get() = withValidityAssertion { _typeArguments }
    }

    public class KtUnresolvedClassTypeQualifier(
        private val _name: Name,
        private val _typeArguments: List<KtTypeProjection>,
        override val token: KtLifetimeToken
    ) : KtClassTypeQualifier {
        override val name: Name get() = withValidityAssertion { _name }
        override val typeArguments: List<KtTypeProjection> get() = withValidityAssertion { _typeArguments }
    }
}

