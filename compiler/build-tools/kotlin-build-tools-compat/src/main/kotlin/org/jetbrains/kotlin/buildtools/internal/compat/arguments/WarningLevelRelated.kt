/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.compat.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments

@OptIn(ExperimentalCompilerArgument::class)
internal fun CommonCompilerArguments.applyWarningLevels(levels: List<WarningLevel>) {
    this.warningLevels = levels.map { item -> "${item.warningName}:${item.severity.stringValue}" }.toTypedArray()
}

@OptIn(ExperimentalCompilerArgument::class)
internal fun applyWarningLevels(
    currentValue: List<WarningLevel>,
    compilerArgs: CommonCompilerArguments,
): List<WarningLevel> =
    compilerArgs.warningLevels.mapOrEmpty { item ->
        val parts = item.split(":", limit = 2)
        require(parts.size == 2) { "Invalid -Xwarning-level format: $item" }
        val severity = WarningLevel.Severity.entries.firstOrNull { entry -> entry.stringValue == parts[1] }
            ?: throw CompilerArgumentsParseException("Unknown -Xwarning-level level: $item")
        WarningLevel(parts[0], severity)
    }