/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.test.directives.TargetPlatformEnum
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
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
    fun compileTestModuleToLibrary(module: TestModule, dependencyBinaryRoots: Collection<Path>, testServices: TestServices): CompilationResult {
        val byRoot = module.files.groupBy { it.directives[Directives.BINARY_ROOT].singleOrNull() }
        val binary = mutableListOf<Path>()
        val sources = mutableListOf<Path>()
        byRoot.entries.forEach { (binaryRootName, files) ->
            val tmpDir = KtTestUtil.tmpDir("testSourcesToCompile").toPath().let { if (binaryRootName != null) it / binaryRootName else it }
            files.forEach { testFile ->
                val text = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                val filePath = tmpDir / testFile.relativePath
                filePath.parent.createDirectories()

                val tmpSourceFile = filePath.createFile()
                tmpSourceFile.writeText(text)
            }

            binary.add(compile(tmpDir, module, dependencyBinaryRoots, testServices))
            sources.add(compileSources(files, module, testServices))
        }
        return CompilationResult(binary, sources)
    }

    data class CompilationResult(val binaries: List<Path>, val sources: List<Path>)

    abstract fun compile(
        tmpDir: Path,
        module: TestModule,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
    ): Path

    abstract fun compileSources(files: List<TestFile>, module: TestModule, testServices: TestServices): Path

    object Directives : SimpleDirectivesContainer() {
        val COMPILER_ARGUMENTS by stringDirective("List of additional compiler arguments")
        val COMPILATION_ERRORS by directive("Is compilation errors expected in the file")
        val LIBRARY_PLATFORMS by enumDirective<TargetPlatformEnum>("Target platforms allowed for library compilation")
        val BINARY_ROOT by stringDirective("A library root to which a file will be compiled", DirectiveApplicability.File)
    }
}
