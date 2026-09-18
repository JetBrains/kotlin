/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal.arguments

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WarningLevel
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments

internal fun CommonCompilerArguments.applyWarningLevels(levels: List<WarningLevelImpl>) {
    this.warningLevels = levels.map { item -> "${item.warningName}:${item.severity.stringValue}" }.toTypedArray()
}

internal fun applyWarningLevels(
    currentValue: List<WarningLevelImpl>,
    compilerArgs: CommonCompilerArguments,
): List<WarningLevelImpl> =
    compilerArgs.warningLevels.mapOrEmpty { item ->
        val parts = item.split(":", limit = 2)
        if (parts.size != 2) {
            throw CompilerArgumentsParseException("Invalid -Xwarning-level format: $item")
        }
        val severity = WarningLevelImpl.Severity.entries.firstOrNull { entry -> entry.stringValue == parts[1] }
            ?: throw CompilerArgumentsParseException("Unknown -Xwarning-level level: $item")
        WarningLevelImpl(parts[0], severity)
    }

@Serializable
internal class WarningLevelImpl(
    val warningName: String,
    val severity: Severity,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WarningLevelImpl) return false
        if (warningName != other.warningName) return false
        return severity == other.severity
    }

    override fun hashCode(): Int {
        var result = warningName.hashCode()
        result = 31 * result + severity.hashCode()
        return result
    }

    override fun toString(): String = "WarningLevel(warningName=$warningName, severity=$severity)"

    enum class Severity(
        val stringValue: String,
    ) {
        ERROR(stringValue = "error"),
        WARNING(stringValue = "warning"),
        DISABLED(stringValue = "disabled");

        fun toApi(): WarningLevel.Severity {
            return WarningLevel.Severity.valueOf(this.name)
        }
    }

    fun toApi(): WarningLevel = WarningLevel(warningName, severity.toApi())
}

internal fun WarningLevel.toImpl(): WarningLevelImpl = WarningLevelImpl(warningName, severity.toImpl())

internal fun WarningLevel.Severity.toImpl(): WarningLevelImpl.Severity = WarningLevelImpl.Severity.valueOf(this.name)
