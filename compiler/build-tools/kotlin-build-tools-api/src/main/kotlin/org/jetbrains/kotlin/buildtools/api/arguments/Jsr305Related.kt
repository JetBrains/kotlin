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
    public class Global(override val mode: Mode) : Jsr305()

    /**
     * Corresponds to -Xjsr305=under-migration:{ignore/strict/warn}
     */
    public class UnderMigration(override val mode: Mode) : Jsr305()

    /**
     * Corresponds to -Xjsr305=@<fq.name>:{ignore/strict/warn}
     */
    public class SpecificAnnotation(public val fqName: String, override val mode: Mode) : Jsr305() {
        public val annotationFqName: String = "@$fqName"
    }

    public enum class Mode(
        public val stringValue: String,
    ) {
        IGNORE(stringValue = "ignore"),
        STRICT(stringValue = "strict"),
        WARN(stringValue = "warn");
    }
}
