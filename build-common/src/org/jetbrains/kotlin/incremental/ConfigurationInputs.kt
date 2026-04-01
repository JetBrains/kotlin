/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import java.io.Serializable

data class ConfigurationInputs(
    /**
     * Stable snapshot of IC configuration keys that affect compilation outcome.
     */
    val icConfigurationInputsSnapshot: Map<String, String?>,
    /**
     * Stable snapshot of compiler arguments that affect compilation outcome (ignoring arguments like, for example, `-version`).
     */
    val compilerArgumentsInputsSnapshot: List<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID = 2L
    }
}
