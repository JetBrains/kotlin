/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.testFramework

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend

interface FrontendBackendConfiguration {
    val useFir: Boolean
        get() = false
    val firParser: FirParser
        get() = FirParser.Psi
    val backend
        get() = TargetBackend.ANY

    fun configureIrFir(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, backend.isIR)
        configuration.put(CommonConfigurationKeys.USE_FIR, useFir)
        when (firParser) {
            FirParser.LightTree -> configuration.put(CommonConfigurationKeys.USE_LIGHT_TREE, true)
            FirParser.Psi -> {}
        }
    }
}
