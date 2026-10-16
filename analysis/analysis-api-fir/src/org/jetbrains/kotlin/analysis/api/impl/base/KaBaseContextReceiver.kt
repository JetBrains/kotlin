/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.Name

@KaExperimentalApi
@KaImplementationDetail
class KaBaseContextReceiver(
    type: KaType,
    label: Name?,
    override val token: KaLifetimeToken,
) : KaContextReceiver() {
    private val backingLabel: Name? = label
    private val backingType: KaType = type

    override val label: Name? get() = withValidityAssertion { backingLabel }
    override val type: KaType get() = withValidityAssertion { backingType }
}