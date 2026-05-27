/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.config

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JKlibConfigurationKeys {
    val JKLIB_OUTPUT_DESTINATION = CompilerConfigurationKey.create<String>("jklib output destination")
}

var CompilerConfiguration.jklibOutputDestination: String?
    get() = get(JKlibConfigurationKeys.JKLIB_OUTPUT_DESTINATION)
    set(value) {
        putIfNotNull(JKlibConfigurationKeys.JKLIB_OUTPUT_DESTINATION, value)
    }
