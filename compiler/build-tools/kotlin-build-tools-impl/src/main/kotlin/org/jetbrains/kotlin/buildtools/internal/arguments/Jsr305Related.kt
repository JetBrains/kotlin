/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.arguments

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

internal fun K2JVMCompilerArguments.applyJsr305(settings: List<Jsr305Impl>) {
    this.jsr305 = settings.map { item ->
        when (item) {
            is Jsr305Impl.Global -> item.mode.stringValue
            is Jsr305Impl.UnderMigration -> "under-migration:${item.mode.stringValue}"
            is Jsr305Impl.SpecificAnnotation -> "${item.annotationFqName}:${item.mode.stringValue}"
        }
    }.toTypedArray()
}

internal fun applyJsr305(
    currentValue: List<Jsr305Impl>,
    compilerArgs: K2JVMCompilerArguments,
): List<Jsr305Impl> =
    compilerArgs.jsr305.mapOrEmpty { fullEntry ->
        val parts = fullEntry.split(":")
        when (parts.size) {
            1 -> Jsr305Impl.Global(jsr305mode(parts[0], fullEntry))
            2 -> {
                if (parts[0] == "under-migration") {
                    Jsr305Impl.UnderMigration(jsr305mode(parts[1], fullEntry))
                } else {
                    Jsr305Impl.SpecificAnnotation(parts[0].removePrefix("@"), jsr305mode(parts[1], fullEntry))
                }
            }
            else -> throw CompilerArgumentsParseException("Invalid -Xjsr305 format: $fullEntry")
        }
    }

private fun jsr305mode(mode: String, fullEntry: String) = Jsr305Impl.Mode.entries.firstOrNull { entry -> entry.stringValue == mode }
    ?: throw CompilerArgumentsParseException("Unknown -Xjsr305 mode: $fullEntry")

@Serializable
public sealed class Jsr305Impl {
    public abstract val mode: Mode

    public class Global(override val mode: Mode) : Jsr305Impl() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Global) return false
            return mode == other.mode
        }

        override fun hashCode(): Int = mode.hashCode()

        override fun toString(): String = "Global(mode=$mode)"
    }

    public class UnderMigration(override val mode: Mode) : Jsr305Impl() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UnderMigration) return false
            return mode == other.mode
        }

        override fun hashCode(): Int = mode.hashCode()

        override fun toString(): String = "UnderMigration(mode=$mode)"
    }

    public class SpecificAnnotation(public val fqName: String, override val mode: Mode) : Jsr305Impl() {
        public val annotationFqName: String = "@$fqName"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SpecificAnnotation) return false
            if (fqName != other.fqName) return false
            return mode == other.mode
        }

        override fun hashCode(): Int {
            var result = fqName.hashCode()
            result = 31 * result + mode.hashCode()
            return result
        }

        override fun toString(): String = "SpecificAnnotation(fqName=$fqName, mode=$mode)"
    }

    public enum class Mode(
        public val stringValue: String,
    ) {
        IGNORE(stringValue = "ignore"),
        STRICT(stringValue = "strict"),
        WARN(stringValue = "warn");

        internal fun toApi(): Jsr305.Mode = Jsr305.Mode.valueOf(name)
    }

    internal fun toApi(): Jsr305 = when (this) {
        is Global -> Jsr305.Global(mode.toApi())
        is SpecificAnnotation -> Jsr305.SpecificAnnotation(fqName, mode.toApi())
        is UnderMigration -> Jsr305.UnderMigration(mode.toApi())
    }
}

internal fun Jsr305.Mode.toImpl(): Jsr305Impl.Mode = Jsr305Impl.Mode.valueOf(name)

internal fun Jsr305.toImpl(): Jsr305Impl = when (this) {
    is Jsr305.Global -> Jsr305Impl.Global(mode.toImpl())
    is Jsr305.SpecificAnnotation -> Jsr305Impl.SpecificAnnotation(fqName, mode.toImpl())
    is Jsr305.UnderMigration -> Jsr305Impl.UnderMigration(mode.toImpl())
}
