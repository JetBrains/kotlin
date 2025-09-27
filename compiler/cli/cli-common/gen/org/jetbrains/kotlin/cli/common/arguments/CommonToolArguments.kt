/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import java.io.Serializable

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/CommonToolArguments.kt
// DO NOT MODIFY IT MANUALLY.

abstract class CommonToolArguments : Freezable(), Serializable {
    @Argument(
        value = "-Werror",
        description = "Report an error if there are any warnings.",
    )
    var allWarningsAsErrors: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Wextra",
        description = "Enable extra checkers for K2.",
    )
    var extraWarnings: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-X",
        description = "Print a synopsis of advanced options.",
    )
    var extraHelp: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-help",
        shortName = "-h",
        description = "Print a synopsis of standard options.",
    )
    var help: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-nowarn",
        description = "Don't generate any warnings.",
    )
    var suppressWarnings: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-verbose",
        description = "Enable verbose logging output.",
    )
    var verbose: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-version",
        description = "Display the compiler version.",
    )
    var version: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    var freeArgs: List<String> = emptyList()
        set(value) {
            checkFrozen()
            field = value
        }

    var internalArguments: List<ManualLanguageFeatureSetting> = emptyList()
        set(value) {
            checkFrozen()
            field = value
        }

    @Transient
    var errors: ArgumentParseErrors? = null

}
