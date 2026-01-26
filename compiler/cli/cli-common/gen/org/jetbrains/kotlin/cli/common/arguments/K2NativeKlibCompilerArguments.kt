/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/NativeKlibCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

class K2NativeKlibCompilerArguments : CommonNativeCompilerArguments() {
    @Argument(
        value = "-Xnative-platform-libraries-path",
        valueDescription = "<path>",
        description = "Path to the Native platform libraries directory.",
    )
    var nativePlatformLibrariesPath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xnative-stdlib-path",
        valueDescription = "<path>",
        description = "Path to the Native stdlib klib.",
    )
    var nativeStdlibPath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @get:Transient
    @field:kotlin.jvm.Transient
    override val configurator: CommonCompilerArgumentsConfigurator = K2NativeKlibCompilerArgumentsConfigurator()

    override fun copyOf(): Freezable = copyK2NativeKlibCompilerArguments(this, K2NativeKlibCompilerArguments())
}
