/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.compat.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

@OptIn(ExperimentalCompilerArgument::class)
internal fun K2JVMCompilerArguments.applyJsr305(settings: List<Jsr305>) {
    this.jsr305 = settings.map { item ->
        when (item) {
            is Jsr305.Global -> item.mode.stringValue
            is Jsr305.UnderMigration -> "under-migration:${item.mode.stringValue}"
            is Jsr305.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
        }
    }.toTypedArray()
}

@OptIn(ExperimentalCompilerArgument::class)
internal fun applyJsr305(
    currentValue: List<Jsr305>,
    compilerArgs: K2JVMCompilerArguments,
): List<Jsr305> =
    compilerArgs.jsr305.mapOrEmpty { fullEntry ->
        val parts = fullEntry.split(":")
        when (parts.size) {
            1 -> Jsr305.Global(jsr305mode(parts[0], fullEntry))
            2 -> {
                if (parts[0] == "under-migration") {
                    Jsr305.UnderMigration(jsr305mode(parts[1], fullEntry))
                } else {
                    Jsr305.SpecificAnnotation(parts[0].removePrefix("@"), jsr305mode(parts[1], fullEntry))
                }
            }
            else -> throw CompilerArgumentsParseException("Invalid -Xjsr305 format: $fullEntry")
        }
    }

private fun jsr305mode(mode: String, fullEntry: String) = Jsr305.Mode.entries.firstOrNull { entry -> entry.stringValue == mode }
    ?: throw CompilerArgumentsParseException("Unknown -Xjsr305 mode: $fullEntry")
