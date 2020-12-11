/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil.compileJavaFilesLibraryToJar
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

abstract class AbstractForeignAnnotationsCompiledJavaDiagnosticTest : AbstractDiagnosticsTest() {
    @OptIn(ExperimentalPathApi::class)
    override fun doMultiFileTest(
        wholeFile: File,
        files: List<TestFile>
    ) {
        val ktFiles = files.filter { !it.name.endsWith(".java") }

        val dir = createTempDirectory()
        val javaFile = File("${wholeFile.parentFile.path}/${wholeFile.nameWithoutExtension}.java")

        File("$dir/${wholeFile.nameWithoutExtension}.java").apply { createNewFile() }.writeText(javaFile.readText())
        File(FOREIGN_JDK8_ANNOTATIONS_SOURCES_PATH).copyRecursively(File("$dir/annotations/"))

        super.doMultiFileTest(
            wholeFile,
            ktFiles,
            compileJavaFilesLibraryToJar(dir.toString(), "foreign-annotations"),
            usePsiClassFilesReading = false,
            excludeNonTypeUseJetbrainsAnnotations = true
        )
    }

    override fun doTest(filePath: String) {
        val file = File(filePath)
        val expectedText = KotlinTestUtils.doLoadFile(file)

        doMultiFileTest(file, createTestFilesFromFile(file, expectedText))
    }
}
