/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.Name

@KaImplementationDetail
class KaBaseFunctionValueParameter(
    name: Name?,
    private val backingType: KaType,
) : KaFunctionValueParameter() {
    override val type: KaType
        get() = withValidityAssertion { backingType }
    override val name: Name? by validityAsserted(name)
    override val token: KaLifetimeToken
        get() = backingType.token

    override fun toString(): String = "KaBaseFunctionValueParameter [name: $name, type: ${type.symbol?.classId}]"
}