/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.NullabilityAnnotation
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

@OptIn(ExperimentalCompilerArgument::class)
internal fun K2JVMCompilerArguments.applyNullabilityAnnotations(settings: List<NullabilityAnnotation>) {
    this.nullabilityAnnotations = settings.map { item -> "${item.annotationFqName}:${item.mode.stringValue}" }.toTypedArray()
}

@OptIn(ExperimentalCompilerArgument::class)
internal fun applyNullabilityAnnotations(
    currentValue: List<NullabilityAnnotation>,
    compilerArgs: K2JVMCompilerArguments,
): List<NullabilityAnnotation> =
    compilerArgs.nullabilityAnnotations.mapOrEmpty { item ->
        val parts = item.split(":")
        if (parts.size != 2) {
            throw CompilerArgumentsParseException("Invalid -Xnullability-annotations format: $item")
        }

        val mode =
            NullabilityAnnotation.Mode.entries.firstOrNull { entry -> entry.stringValue == parts[1] }
                ?: throw CompilerArgumentsParseException("Unknown -Xnullability-annotations mode: $item")
        NullabilityAnnotation(parts[0].removePrefix("@"), mode)
    }
