/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/K2NativeKlibCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

abstract class K2NativeKlibCompilerArguments : CommonKlibBasedCompilerArguments() {
    @Argument(
        value = "-produce",
        shortName = "-p",
        valueDescription = "{program|static|dynamic|framework|library|bitcode}",
        description = "Specify the output file kind.",
    )
    var produce: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-target",
        valueDescription = "<target>",
        description = "Set the hardware target.",
    )
    var target: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

}
