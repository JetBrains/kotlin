/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object NativeKlibConfigurationKeys {
    @JvmField
    val NATIVE_STDLIB_PATH = CompilerConfigurationKey.create<String>("Path to Native stdlib klib")

    @JvmField
    val NATIVE_PLATFORM_LIBRARIES_PATH = CompilerConfigurationKey.create<String>("Path to Native platform libraries directory")
}
