/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

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
        val javaFile = File("${wholeFile.parentFile.path}/${wholeFile.nameWithoutExtension}.java")

        assertExists(javaFile)

        val javaFilesDir = createTempDirectory().toFile().also {
            File(FOREIGN_JDK8_ANNOTATIONS_SOURCES_PATH).copyRecursively(File("$it/annotations/"))
            javaFile.copyTo(File("$it/${javaFile.name}"))
        }

        super.doMultiFileTest(
            wholeFile,
            ktFiles,
            compileJavaFilesLibraryToJar(javaFilesDir.path, "java-files"),
            usePsiClassFilesReading = false,
            excludeNonTypeUseJetbrainsAnnotations = true
        )
    }
}
