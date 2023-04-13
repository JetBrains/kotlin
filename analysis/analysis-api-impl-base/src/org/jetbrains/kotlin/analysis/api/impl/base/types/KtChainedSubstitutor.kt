/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.types

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType

class KtChainedSubstitutor(val first: KtSubstitutor, val second: KtSubstitutor) : KtSubstitutor {
    override val token: KtLifetimeToken get() = first.token
    override fun substituteOrNull(type: KtType): KtType? = first.substituteOrNull(type)?.let { second.substituteOrNull(it) }
}