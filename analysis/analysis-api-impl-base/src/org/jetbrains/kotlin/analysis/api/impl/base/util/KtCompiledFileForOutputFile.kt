/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.components.KtCompiledFile
import org.jetbrains.kotlin.backend.common.output.OutputFile
import java.io.File

@KtAnalysisApiInternals
class KtCompiledFileForOutputFile(private val outputFile: OutputFile) : KtCompiledFile {
    override val path: String
        get() = outputFile.relativePath

    override val sourceFiles: List<File>
        get() = outputFile.sourceFiles

    override val content: ByteArray
        get() = outputFile.asByteArray()
}