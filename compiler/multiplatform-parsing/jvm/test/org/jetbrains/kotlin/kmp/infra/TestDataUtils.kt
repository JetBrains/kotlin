/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.toSourceLinesMapping
import java.io.File
import java.nio.file.Path

object TestDataUtils {
    val testDataDirs: List<File> = System.getProperty("test.data.dirs").split(File.pathSeparator).map { File(it) }

    // TODO: for some reason, it's not possible to depend on `:compiler:test-infrastructure-utils` here
    // See org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
    private val openingDiagnosticRegex = """(<!([^"]*?((".*?")(, ".*?")*?)?[^"]*?)!>)""".toRegex()
    private val closingDiagnosticRegex = """(<!>)""".toRegex()

    private val xmlLikeTagsRegex = """(</?(?:selection|expr|caret)>)""".toRegex()

    private val allMetadataRegex =
        """(${closingDiagnosticRegex.pattern}|${openingDiagnosticRegex.pattern}|${xmlLikeTagsRegex.pattern})""".toRegex()

    fun checkKotlinFiles(kotlinFileChecker: (String, Path, KtSourceFileLinesMapping) -> Unit) {
        for (testDataDir in testDataDirs) {
            testDataDir.walkTopDown()
                .filter { it.isFile && it.extension.let { ext -> ext == "kt" || ext == "kts" || ext == "nkt" } }
                .forEach {
                    val refinedText = it.readText().replace(allMetadataRegex, "")
                    kotlinFileChecker(refinedText, it.toPath(), refinedText.toSourceLinesMapping())
                }
        }
    }
}

