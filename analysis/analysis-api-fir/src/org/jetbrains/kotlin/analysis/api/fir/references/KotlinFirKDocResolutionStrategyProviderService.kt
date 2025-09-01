/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.util.registry.RegistryValueListener
import org.jetbrains.kotlin.references.utils.KotlinKDocResolutionStrategyProviderService

internal class KotlinFirKDocResolutionStrategyProviderService : KotlinKDocResolutionStrategyProviderService {
    @Volatile
    private var shouldUseExperimentalResolution: Boolean = false

    init {
        val enableExperimentalKDocResolutionKey = "kotlin.analysis.experimentalKDocResolution"
        shouldUseExperimentalResolution = Registry.`is`(enableExperimentalKDocResolutionKey, false)

        Registry.get(enableExperimentalKDocResolutionKey).addListener(object : RegistryValueListener {
            override fun afterValueChanged(value: RegistryValue) {
                shouldUseExperimentalResolution = Registry.`is`(enableExperimentalKDocResolutionKey, false)
            }
        }, this)
    }

    override fun shouldUseExperimentalStrategy(): Boolean = shouldUseExperimentalResolution

    override fun dispose() {}
}