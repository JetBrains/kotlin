/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.KtInMemoryTextSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

abstract class SourceFilePreprocessor(val testServices: TestServices) {
    abstract fun process(file: TestFile, content: String): String

    /**
     * Override this method only if your preprocessor transformation depends on the whole module.
     * If each file could be transformed independently, override [process] instead.
     */
    @TestInfrastructureInternals
    open fun processModule(module: TestModule, filesContent: MutableMap<TestFile, String>) {
        filesContent.replaceAll { file, value -> process(file, value) }
    }
}

abstract class ReversibleSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    abstract fun revert(file: TestFile, actualContent: String): String
}

abstract class SourceFileProvider : TestService {
    abstract val preprocessors: List<SourceFilePreprocessor>

    abstract fun getKotlinSourceDirectoryForModule(module: TestModule): File
    abstract fun getJavaSourceDirectoryForModule(module: TestModule): File
    abstract fun getAdditionalFilesDirectoryForModule(module: TestModule): File

    abstract fun getContentOfSourceFile(testFile: TestFile, preprocessorFilter: ((SourceFilePreprocessor) -> Boolean)? = null): String
    abstract fun getOrCreateRealFileForSourceFile(testFile: TestFile): File
}

val TestServices.sourceFileProvider: SourceFileProvider by TestServices.testServiceAccessor()

class SourceFileProviderImpl(
    val testServices: TestServices,
    override val preprocessors: List<SourceFilePreprocessor>,
) : SourceFileProvider() {
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

    @OptIn(TestInfrastructureInternals::class)
    override fun getContentOfSourceFile(
        testFile: TestFile,
        preprocessorFilter: ((SourceFilePreprocessor) -> Boolean)?,
    ): String {
        val defaultMode = preprocessorFilter == null
        if (defaultMode) {
            contentOfFiles[testFile]?.let { return it }
        }

        // Usually all files belong to some module. But some services (like `FirTestDataConsistencyHandler`)
        // create test files on the fly which don't have a containing module.
        // So for them, we also need to create a module on the fly.
        val module = testServices.moduleStructure.modules.singleOrNull { testFile in it.files } ?: TestModule(
            name = "_stubModuleForOnTheFlyFile_",
            files = listOf(testFile),
            allDependencies = emptyList(),
            directives = RegisteredDirectives.Empty,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
        )

        val contentPerFile = module.files.associateWithTo(mutableMapOf()) { it.originalContent }
        val preprocessors = if (defaultMode) preprocessors else preprocessors.filter(preprocessorFilter)
        for (preprocessor in preprocessors) {
            preprocessor.processModule(module, contentPerFile)
        }

        if (defaultMode) {
            contentOfFiles.putAll(contentPerFile)
        }
        return contentPerFile.getValue(testFile)
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

fun SourceFileProvider.getKtFilesForSourceFiles(
    testFiles: Collection<TestFile>,
    project: Project,
    findViaVfs: Boolean = false,
    keepNonKtFiles: Boolean = false,
): Map<TestFile, KtFile> {
    return testFiles.mapNotNull {
        if (!keepNonKtFiles && !it.isKtFile) return@mapNotNull null
        it to getKtFileForSourceFile(it, project, findViaVfs)
    }.toMap()
}

fun TestFile.toLightTreeShortName() = name.substringAfterLast('/').substringAfterLast('\\')

fun SourceFileProvider.getKtSourceFilesForSourceFiles(
    testFiles: Collection<TestFile>,
    keepNonKtFiles: Boolean,
): Map<TestFile, KtSourceFile> {
    return testFiles.mapNotNull {
        if (!keepNonKtFiles && !it.isKtFile) return@mapNotNull null
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

fun TestModule.independentSourceDirectoryPath(testServices: TestServices): String {
    val path = testServices.sourceFileProvider.getKotlinSourceDirectoryForModule(this).canonicalPath
    return FileUtil.toSystemIndependentName(path)
}

fun TestModule.independentSourceDirectoryPathsTransitive(testServices: TestServices): List<String> {
    return buildList {
        addAll(transitiveRegularDependencies(includeSelf = true))
        addAll(transitiveFriendDependencies(includeSelf = false))
        addAll(transitiveDependsOnDependencies(includeSelf = false))
    }.map {
        it.independentSourceDirectoryPath(testServices)
    }
}
