/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaDeprecation

@KaImplementationDetail
@KaExperimentalApi
class KaBaseDeprecation(
    override val level: KaDeprecation.Level,
    override val isPropagatedToOverrides: Boolean
) : KaDeprecation {
    override fun toString(): String {
        return "KaDeprecation(level=$level, isPropagatedToOverrides=$isPropagatedToOverrides)"
    }
}
