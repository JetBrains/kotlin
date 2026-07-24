/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.analysis.test.framework.services.TargetPlatformEnum
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

abstract class TestModuleCompiler : TestService {
    fun compileTestModuleToLibrary(
        module: TestModule,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
    ): CompilationResult {
        val binary = mutableListOf<Path>()
        val sources = mutableListOf<Path>()

        val commonTestFiles = computeCommonFiles(module, testServices)

        fun writeFiles(testFiles: List<TestFile>): Path? {
            if (testFiles.isEmpty()) return null
            val tmpDirectory = createTemporaryPath()
            testFiles.forEach { write(it, testServices, tmpDirectory) }
            return tmpDirectory
        }

        val filesByBinaryRoot = module.files.groupBy { it.directives[Directives.BINARY_ROOT].singleOrNull() }
        for ([binaryRootName, files] in filesByBinaryRoot) {
            // Resource files are not compiled. They are embedded into the produced library by `compile`.
            val [resourceFiles, sourceFiles] = files.partition { Directives.LIBRARY_RESOURCE in it.directives }
            val sourcesTempDirectory = writeFiles(sourceFiles) ?: error("No sources found")
            val commonSourcesTestDirectory = writeFiles(commonTestFiles)

            val libraryName = buildString {
                append(module.name)
                if (binaryRootName != null) {
                    append("-")
                    append(binaryRootName)
                }
            }

            binary.add(
                compile(
                    sourcesTempDirectory,
                    commonSourcesTestDirectory,
                    module,
                    libraryName,
                    dependencyBinaryRoots,
                    resourceFiles,
                    testServices,
                )
            )
            sources.add(compileSources(sourceFiles, module, testServices))
        }

        return CompilationResult(binary, sources)
    }

    private fun computeCommonFiles(module: TestModule, testServices: TestServices): List<TestFile> {
        val dependsOnDependencies = module.dependsOnDependencies
        if (dependsOnDependencies.isEmpty()) {
            return emptyList()
        }

        val selfTargetPlatform = module.targetPlatform(testServices)
        require(!selfTargetPlatform.isCommon()) {
            "${module.name} is expected to be an implementing module while its platform is common"
        }

        return buildList {
            for (dependsOnDependency in dependsOnDependencies) {
                val dependencyModule = dependsOnDependency.dependencyModule
                val dependencyTargetPlatform = dependencyModule.targetPlatform(testServices)
                require(dependencyTargetPlatform.isCommon()) {
                    "${module.name} is expected to be a common module while it's platform is $dependencyTargetPlatform"
                }

                addAll(dependencyModule.files)
            }
        }
    }

    private fun createTemporaryPath(): Path {
        return KtTestUtil.tmpDir("testSourcesToCompile").toPath()
    }

    private fun write(testFile: TestFile, testServices: TestServices, target: Path): Path {
        val text = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
        val filePath = target.resolve(testFile.relativePath)

        filePath.parent.createDirectories()
        filePath.writeText(text)
        return filePath
    }

    data class CompilationResult(val binaries: List<Path>, val sources: List<Path>)

    abstract fun compile(
        sourcesTempDirectory: Path,
        commonSourcesTempDirectory: Path?,
        module: TestModule,
        libraryName: String,
        dependencyBinaryRoots: Collection<Path>,
        resourceFiles: List<TestFile>,
        testServices: TestServices,
    ): Path

    abstract fun compileSources(files: List<TestFile>, module: TestModule, testServices: TestServices): Path

    object Directives : SimpleDirectivesContainer() {
        val COMPILER_ARGUMENTS by stringDirective("List of additional compiler arguments")
        val COMPILATION_ERRORS by directive("Is compilation errors expected in the file")
        val LIBRARY_PLATFORMS by enumDirective<TargetPlatformEnum>("Target platforms allowed for library compilation")
        val BINARY_ROOT by stringDirective("A library root to which a file will be compiled", DirectiveApplicability.File)

        val LIBRARY_RESOURCE by directive(
            "Embeds the file verbatim as a resource in the compiled library, regardless of its extension. Such a file is not compiled.",
            DirectiveApplicability.File,
        )
    }
}
