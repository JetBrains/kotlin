/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseExplicitReceiverValue(
    expression: KtExpression,
    private val backingType: KaType,
    isSafeNavigation: Boolean,
) : KaExplicitReceiverValue {
    private val backingExpression: KtExpression = expression
    private val backingIsSafeNavigation: Boolean = isSafeNavigation
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }
    override val expression: KtExpression get() = withValidityAssertion { backingExpression }
    override val isSafeNavigation: Boolean get() = withValidityAssertion { backingIsSafeNavigation }
}
