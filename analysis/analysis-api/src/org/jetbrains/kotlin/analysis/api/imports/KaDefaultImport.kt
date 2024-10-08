/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.imports

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * Represents a default import that the Kotlin compiler adds to each file during resolution.
 *
 * See [Kotlin Language Specification](https://kotlinlang.org/spec/packages-and-imports.html).
 * @see [KaDefaultImportsProvider]
 */
@KaIdeApi
public interface KaDefaultImport {
    /**
     * The path that is imported by default.
     *
     * It may be a star import if [ImportPath.isAllUnder] is `true`, or a non-star import if `false`.
     */
    public val importPath: ImportPath

    /**
     * Represents the priority of the current default import.
     *
     * @see [KaDefaultImportPriority]
     */
    public val priority: KaDefaultImportPriority
}

/**
 * Represents the priority of a default import.
 *
 * In the case of name conflicts, higher priority wins during resolution.
 */
@KaIdeApi
public enum class KaDefaultImportPriority {
    LOW,
    HIGH,
}