/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.ALL
import org.jetbrains.kotlin.cli.common.arguments.DevModeOverwritingStrategies.OLDER

class K2JSDceArguments : CommonToolArguments() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @GradleOption(
        value = DefaultValue.STRING_NULL_DEFAULT,
        gradleInputType = GradleInputTypes.INTERNAL, // handled by 'destinationDirectory'
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @GradleDeprecatedOption(
        message = "Use task 'destinationDirectory' to configure output directory",
        level = DeprecationLevel.WARNING,
        removeAfter = "1.9.0"
    )
    @Argument(
            value = "-output-dir",
            valueDescription = "<path>",
            description = "Output directory"
    )
    var outputDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
            value = "-keep",
            valueDescription = "<fully.qualified.name[,]>",
            description = "List of fully-qualified names of declarations that shouldn't be eliminated"
    )
    var declarationsToKeep: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
            value = "-Xprint-reachability-info",
            description = "Print declarations marked as reachable"
    )
    var printReachabilityInfo = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
            value = "-dev-mode",
            description = "Development mode: don't strip out any code, just copy dependencies"
    )
    var devMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdev-mode-overwriting-strategy",
        valueDescription = "{$OLDER|$ALL}",
        description = "Overwriting strategy during copy dependencies in development mode"
    )
    var devModeOverwritingStrategy: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    override fun copyOf(): Freezable = copyK2JSDceArguments(this, K2JSDceArguments())
}

object DevModeOverwritingStrategies {
    const val OLDER = "older"
    const val ALL = "all"
}
