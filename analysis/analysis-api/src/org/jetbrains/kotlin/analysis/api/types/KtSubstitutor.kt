/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion


public interface KtSubstitutor : ValidityTokenOwner {
    public fun substituteOrSelf(type: KtType): KtType = substituteOrNull(type) ?: type
    public fun substituteOrNull(type: KtType): KtType?

    public class Empty(override val token: ValidityToken) : KtSubstitutor {
        override fun substituteOrNull(type: KtType): KtType = withValidityAssertion { type }
    }
}
