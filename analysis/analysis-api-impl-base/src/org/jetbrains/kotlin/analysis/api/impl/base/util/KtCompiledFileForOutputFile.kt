/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.components.KaCompiledFile
import org.jetbrains.kotlin.backend.common.output.OutputFile
import java.io.File

@KaAnalysisApiInternals
class KaCompiledFileForOutputFile(private val outputFile: OutputFile) : KaCompiledFile {
    override val path: String
        get() = outputFile.relativePath

    override val sourceFiles: List<File>
        get() = outputFile.sourceFiles

    override val content: ByteArray
        get() = outputFile.asByteArray()
}