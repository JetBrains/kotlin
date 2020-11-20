/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys

abstract class AbstractFirCompileKotlinAgainstKotlinTest : AbstractCompileKotlinAgainstKotlinTest() {
    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.put(CommonConfigurationKeys.USE_FIR, true)
        configuration.put(JVMConfigurationKeys.IR, true)
    }
}