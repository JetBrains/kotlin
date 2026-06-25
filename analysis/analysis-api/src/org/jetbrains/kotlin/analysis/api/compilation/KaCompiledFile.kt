/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import java.io.File

/**
 * A single file produced by an in-memory compilation performed by [compile].
 *
 * @see KaCompilationResult.Success
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCompiledFile {
    /**
     * The path of the compiled file relative to the root of the output directory.
     */
    public val path: String

    /**
     * The source files that were compiled to produce this file.
     */
    public val sourceFiles: List<File>

    /**
     * The content of the compiled file.
     */
    public val content: ByteArray
}

/**
 * Whether the compiled file is a Java class file.
 */
@KaExperimentalApi
public val KaCompiledFile.isClassFile: Boolean
    get() = path.endsWith(".class", ignoreCase = true)
