/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.arguments

/**
 * @since 2.4.0
 */
public sealed class Jsr305 {
    public abstract val mode: Mode

    /**
     * Corresponds to -Xjsr305={ignore/strict/warn}
     */
    public class Global(override val mode: Mode) : Jsr305() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Global) return false
            return mode == other.mode
        }

        override fun hashCode(): Int = mode.hashCode()

        override fun toString(): String = "Global(mode=$mode)"
    }

    /**
     * Corresponds to -Xjsr305=under-migration:{ignore/strict/warn}
     */
    public class UnderMigration(override val mode: Mode) : Jsr305() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UnderMigration) return false
            return mode == other.mode
        }

        override fun hashCode(): Int = mode.hashCode()

        override fun toString(): String = "UnderMigration(mode=$mode)"
    }

    /**
     * Corresponds to -Xjsr305=@<fq.name>:{ignore/strict/warn}
     */
    public class SpecificAnnotation(public val fqName: String, override val mode: Mode) : Jsr305() {
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
    }
}
