/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.NullabilityAnnotation
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

internal fun K2JVMCompilerArguments.applyNullabilityAnnotations(settings: List<NullabilityAnnotationImpl>) {
    this.nullabilityAnnotations = settings.map { item -> "${item.annotationFqName}:${item.mode.stringValue}" }.toTypedArray()
}


@OptIn(ExperimentalCompilerArgument::class)
@Suppress("EnumValuesSoftDeprecate")
internal fun applyNullabilityAnnotations(
    currentValue: List<NullabilityAnnotationImpl>,
    compilerArgs: K2JVMCompilerArguments,
): List<NullabilityAnnotationImpl> =
    compilerArgs.nullabilityAnnotations.mapOrEmpty { item ->
        val parts = item.split(":")
        if (parts.size != 2) {
            throw CompilerArgumentsParseException("Invalid -Xnullability-annotations format: $item")
        }

        val mode =
            NullabilityAnnotationImpl.Mode.values().firstOrNull { entry -> entry.stringValue == parts[1] }
                ?: throw CompilerArgumentsParseException("Unknown -Xnullability-annotations mode: $item")
        NullabilityAnnotationImpl(parts[0].removePrefix("@"), mode)
    }

@Serializable
public class NullabilityAnnotationImpl(
    public val fqName: String,
    public val mode: Mode,
) {

    public val annotationFqName: String = "@$fqName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NullabilityAnnotationImpl) return false
        if (fqName != other.fqName) return false
        return mode == other.mode
    }

    override fun hashCode(): Int {
        var result = fqName.hashCode()
        result = 31 * result + mode.hashCode()
        return result
    }

    override fun toString(): String = "NullabilityAnnotation(fqName=$fqName, mode=$mode)"

    public enum class Mode(
        public val stringValue: String,
    ) {
        IGNORE(stringValue = "ignore"),
        STRICT(stringValue = "strict"),
        WARN(stringValue = "warn");

        internal fun toApi(): NullabilityAnnotation.Mode = NullabilityAnnotation.Mode.valueOf(name)
    }

    internal fun toApi(): NullabilityAnnotation = NullabilityAnnotation(fqName, mode.toApi())
}

internal fun NullabilityAnnotation.Mode.toImpl(): NullabilityAnnotationImpl.Mode = NullabilityAnnotationImpl.Mode.valueOf(name)

internal fun NullabilityAnnotation.toImpl(): NullabilityAnnotationImpl = NullabilityAnnotationImpl(fqName, mode.toImpl())
