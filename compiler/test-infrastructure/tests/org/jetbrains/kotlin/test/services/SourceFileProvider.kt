/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class SourceFilePreprocessor(val testServices: TestServices) {
    abstract fun process(file: TestFile, content: String): String
}

abstract class SourceFileProvider : TestService {
    abstract val kotlinSourceDirectory: File
    abstract val javaSourceDirectory: File

    abstract fun getContentOfSourceFile(testFile: TestFile): String
    abstract fun getRealFileForSourceFile(testFile: TestFile): File
}

val TestServices.sourceFileProvider: SourceFileProvider by TestServices.testServiceAccessor()

class SourceFileProviderImpl(val testServices: TestServices, val preprocessors: List<SourceFilePreprocessor>) : SourceFileProvider() {
    override val kotlinSourceDirectory: File = testServices.createTempDirectory("kotlin-files")
    override val javaSourceDirectory: File = testServices.createTempDirectory("java-files")

    private val contentOfFiles = mutableMapOf<TestFile, String>()
    private val realFileMap = mutableMapOf<TestFile, File>()

    override fun getContentOfSourceFile(testFile: TestFile): String {
        return contentOfFiles.getOrPut(testFile) {
            generateFinalContent(testFile)
        }
    }

    override fun getRealFileForSourceFile(testFile: TestFile): File {
        return realFileMap.getOrPut(testFile) {
            val directory = when {
                testFile.isKtFile -> kotlinSourceDirectory
                testFile.isJavaFile -> javaSourceDirectory
                else -> error("Unknown file type: ${testFile.name}")
            }
            directory.resolve(testFile.relativePath).also {
                it.parentFile.mkdirs()
                it.writeText(getContentOfSourceFile(testFile))
            }
        }
    }

    private fun generateFinalContent(testFile: TestFile): String {
        return preprocessors.fold(testFile.originalContent) { content, preprocessor ->
            preprocessor.process(testFile, content)
        }
    }
}

fun SourceFileProvider.getKtFileForSourceFile(testFile: TestFile, project: Project): KtFile {
// TODO
//    return TestCheckerUtil.createCheckAndReturnPsiFile(
    return KtTestUtil.createFile(
        testFile.name,
        getContentOfSourceFile(testFile),
        project
    )
}

fun SourceFileProvider.getKtFilesForSourceFiles(testFiles: Collection<TestFile>, project: Project): Map<TestFile, KtFile> {
    return testFiles.mapNotNull {
        if (!it.isKtFile) return@mapNotNull null
        it to getKtFileForSourceFile(it, project)
    }.toMap()
}

val TestFile.isKtFile: Boolean
    get() = name.endsWith(".kt") || name.endsWith(".kts")

val TestFile.isKtsFile: Boolean
    get() = name.endsWith(".kts")

val TestFile.isJavaFile: Boolean
    get() = name.endsWith(".java")
