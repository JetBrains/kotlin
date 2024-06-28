/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.KtInMemoryTextSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class SourceFilePreprocessor(val testServices: TestServices) {
    abstract fun process(file: TestFile, content: String): String
}

abstract class ReversibleSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    abstract fun revert(file: TestFile, actualContent: String): String
}

abstract class SourceFileProvider : TestService {
    abstract val preprocessors: List<SourceFilePreprocessor>

    abstract fun getKotlinSourceDirectoryForModule(module: TestModule): File
    abstract fun getJavaSourceDirectoryForModule(module: TestModule): File
    abstract fun getAdditionalFilesDirectoryForModule(module: TestModule): File

    abstract fun getContentOfSourceFile(testFile: TestFile): String
    abstract fun getOrCreateRealFileForSourceFile(testFile: TestFile): File
}

val TestServices.sourceFileProvider: SourceFileProvider by TestServices.testServiceAccessor()

class SourceFileProviderImpl(val testServices: TestServices, override val preprocessors: List<SourceFilePreprocessor>) : SourceFileProvider() {
    private val kotlinSourceDirectory: File by lazy(LazyThreadSafetyMode.NONE) { testServices.getOrCreateTempDirectory("kotlin-sources") }
    private val javaSourceDirectory: File by lazy(LazyThreadSafetyMode.NONE) { testServices.getOrCreateTempDirectory("java-sources") }
    private val additionalFilesDirectory: File by lazy(LazyThreadSafetyMode.NONE) { testServices.getOrCreateTempDirectory("additional-files") }

    private val contentOfFiles = mutableMapOf<TestFile, String>()
    private val realFileMap = mutableMapOf<TestFile, File>()

    override fun getKotlinSourceDirectoryForModule(module: TestModule): File =
        kotlinSourceDirectory.resolve(module.name).apply { mkdir() }

    override fun getJavaSourceDirectoryForModule(module: TestModule): File =
        javaSourceDirectory.resolve(module.name).apply { mkdir() }

    override fun getAdditionalFilesDirectoryForModule(module: TestModule): File =
        additionalFilesDirectory.resolve(module.name).apply { mkdir() }

    override fun getContentOfSourceFile(testFile: TestFile): String {
        return contentOfFiles.getOrPut(testFile) {
            preprocessors.fold(testFile.originalContent) { content, preprocessor ->
                preprocessor.process(testFile, content)
            }
        }
    }

    override fun getOrCreateRealFileForSourceFile(testFile: TestFile): File {
        return realFileMap.getOrPut(testFile) {
            val module = testServices.moduleStructure.modules.single { testFile in it.files }
            val directory = when {
                testFile.isKtFile -> getKotlinSourceDirectoryForModule(module)
                testFile.isJavaFile -> getJavaSourceDirectoryForModule(module)
                else -> getAdditionalFilesDirectoryForModule(module)
            }
            directory.resolve(testFile.relativePath).also {
                it.parentFile.mkdirs()
                it.writeText(getContentOfSourceFile(testFile))
            }
        }
    }
}

fun SourceFileProvider.getKtFileForSourceFile(testFile: TestFile, project: Project, findViaVfs: Boolean = false): KtFile {
    if (findViaVfs) {
        val realFile = getOrCreateRealFileForSourceFile(testFile)
        StandardFileSystems.local().findFileByPath(realFile.path)
            ?.let { PsiManager.getInstance(project).findFile(it) as? KtFile }
            ?.let { return it }
    }
    return KtTestUtil.createFile(
        testFile.name,
        getContentOfSourceFile(testFile),
        project
    )
}

fun SourceFileProvider.getKtFilesForSourceFiles(testFiles: Collection<TestFile>, project: Project, findViaVfs: Boolean = false): Map<TestFile, KtFile> {
    return testFiles.mapNotNull {
        if (!it.isKtFile) return@mapNotNull null
        it to getKtFileForSourceFile(it, project, findViaVfs)
    }.toMap()
}

fun TestFile.toLightTreeShortName() = name.substringAfterLast('/').substringAfterLast('\\')

fun SourceFileProvider.getKtSourceFilesForSourceFiles(
    testFiles: Collection<TestFile>,
): Map<TestFile, KtSourceFile> {
    return testFiles.mapNotNull {
        if (!it.isKtFile) return@mapNotNull null
        val shortName = it.toLightTreeShortName()
        val ktSourceFile = KtInMemoryTextSourceFile(shortName, "/$shortName", getContentOfSourceFile(it))
        it to ktSourceFile
    }.toMap()
}

val TestFile.isKtFile: Boolean
    get() = name.endsWith(".kt") || name.endsWith(".kts")

val TestFile.isKtsFile: Boolean
    get() = name.endsWith(".kts")

val TestFile.isJavaFile: Boolean
    get() = name.endsWith(".java")

val TestFile.isModuleInfoJavaFile: Boolean
    get() = name == PsiJavaModule.MODULE_INFO_FILE

val TestFile.isJsFile: Boolean
    get() = name.endsWith(".js")

val TestFile.isMjsFile: Boolean
    get() = name.endsWith(".mjs")

val TestModule.javaFiles: List<TestFile>
    get() = files.filter { it.isJavaFile }

val TestFile.isExternalAnnotation: Boolean
    get() = name == ExternalAnnotationsManager.ANNOTATIONS_XML

fun SourceFileProvider.getRealJavaFiles(module: TestModule): List<File> {
    return module.javaFiles.map { getOrCreateRealFileForSourceFile(it) }
}
