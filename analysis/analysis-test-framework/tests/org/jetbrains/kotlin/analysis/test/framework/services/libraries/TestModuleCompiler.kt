/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.writeText

abstract class TestModuleCompiler : TestService {
    fun compileTestModuleToLibrary(module: TestModule, testServices: TestServices): Path {
        val tmpDir = KtTestUtil.tmpDir("testSourcesToCompile").toPath()
        for (testFile in module.files) {
            val text = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
            val filePath = tmpDir / testFile.relativePath
            filePath.parent.createDirectories()

            val tmpSourceFile = filePath.createFile()
            tmpSourceFile.writeText(text)
        }
        return compile(tmpDir, module, testServices)
    }

    abstract fun compile(tmpDir: Path, module: TestModule, testServices: TestServices): Path

    abstract fun compileTestModuleToLibrarySources(module: TestModule, testServices: TestServices): Path?

    object Directives : SimpleDirectivesContainer() {
        val COMPILER_ARGUMENTS by stringDirective("List of additional compiler arguments")
        val COMPILATION_ERRORS by directive("Is compilation errors expected in the file")
    }
}
