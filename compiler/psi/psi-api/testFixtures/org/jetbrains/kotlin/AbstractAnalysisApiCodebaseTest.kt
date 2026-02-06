/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseTest.SourceDirectory
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

/**
 * Test for checking various aspects of the Analysis API project itself based on PSI of files in the project.
 *
 * Traverses [sourceDirectories] and runs [traverse] on each of them.
 *
 * See [AbstractAnalysisApiCodebaseDumpFileComparisonTest] and [AbstractAnalysisApiCodebaseValidationTest]
 */
abstract class AbstractAnalysisApiCodebaseTest<T : SourceDirectory> : KtUsefulTestCase() {
    protected fun doTest() {
        val environment = createProjectEnvironment(
            CompilerConfiguration.create(),
            testRootDisposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
        )
        val psiManager = PsiManager.getInstance(environment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        sourceDirectories.forEach { sourceDirectory ->
            sourceDirectory.traverse(psiManager, fileSystem)
        }
    }

    abstract fun T.traverse(psiManager: PsiManager, fileSystem: VirtualFileSystem)

    abstract val sourceDirectories: List<T>

    protected fun PsiElement.renderDeclaration(): String =
        getQualifiedName()?.let { fqn ->
            val parameterList = when (this) {
                is KtDeclaration -> getKtParameterList()
                else -> getParameterList()
            }
            fqn + parameterList
        } ?: "RENDERING ERROR"

    protected fun SourceDirectory.getRoots(): List<File> {
        val homeDir = KtTestUtil.getHomeDirectory()
        return this.sourcePaths.map { File(homeDir, it) }
    }

    protected fun File.getPsiFile(psiManager: PsiManager, fileSystem: VirtualFileSystem): PsiFile? {
        if (this.isDirectory) return null
        if (this.extension != "kt" && this.extension != "java") return null
        return createPsiFile(this.path, psiManager, fileSystem)
    }

    sealed class SourceDirectory(val sourcePaths: List<String>) {
        class ForValidation(sourcePaths: List<String>) : SourceDirectory(sourcePaths)
        class ForDumpFileComparison(sourcePaths: List<String>, val outputFilePath: String) : SourceDirectory(sourcePaths)
    }

    private fun PsiElement.getQualifiedName(): String? {
        return when (this) {
            is KtConstructor<*> -> this.containingClassOrObject?.getQualifiedName().let { classFqName ->
                "$classFqName:constructor"
            }
            is KtNamedDeclaration -> this.fqName?.asString()
            is PsiQualifiedNamedElement -> this.qualifiedName
            is PsiJvmMember -> this.containingClass?.qualifiedName?.let { classFqName ->
                val isConstructor = (this as? PsiMethod)?.isConstructor == true
                classFqName + ":" + if (isConstructor) "constructor" else (this.name ?: "")
            }
            else -> null
        }
    }

    private fun PsiElement.getParameterList(): String {
        val parameterList =
            childrenOfType<PsiParameterList>().singleOrNull()
                ?.allChildren
                ?.filterIsInstance<PsiParameter>()?.joinToString(", ") {
                    it.type.presentableText
                }?.let {
                    "($it)"
                } ?: ""

        return parameterList
    }

    private fun KtDeclaration.getKtParameterList(): String {
        val parameterList =
            childrenOfType<KtParameterList>().singleOrNull()
                ?.allChildren
                ?.filterIsInstance<KtParameter>()?.joinToString(", ") {
                    it.typeReference?.typeElement?.text ?: ""
                }?.let {
                    "($it)"
                } ?: ""

        return parameterList
    }

    private fun createPsiFile(fileName: String, psiManager: PsiManager, fileSystem: VirtualFileSystem): PsiFile? {
        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")
        val psiFile = psiManager.findFile(file)
        return psiFile
    }
}

/**
 * Test for checking the code base against the master file.
 *
 * Traverses [sourceDirectories] and triggers [processFile] on each contained file.
 * For each directory builds the resulting text out of lines returned from [processFile].
 * Then compares this text against the contents of the master file [SourceDirectory.ForDumpFileComparison.outputFilePath].
 *
 * If comparison fails, throws an exception with [getErrorMessage] as the message.
 */
abstract class AbstractAnalysisApiCodebaseDumpFileComparisonTest :
    AbstractAnalysisApiCodebaseTest<SourceDirectory.ForDumpFileComparison>() {
    override fun SourceDirectory.ForDumpFileComparison.traverse(psiManager: PsiManager, fileSystem: VirtualFileSystem) {
        val roots = getRoots()

        val actualText = buildList {
            for (root in roots) {
                for (file in root.walkTopDown()) {
                    val psiFile = file.getPsiFile(psiManager, fileSystem) ?: continue
                    addAll(psiFile.processFile())
                }
            }
        }.sorted().joinToString("\n")

        val expectedFile = getExpectedFile()
        val errorMessage = getErrorMessage()
        TestDataAssertions.assertEqualsToFile(errorMessage, expectedFile, actualText)
    }

    private fun SourceDirectory.ForDumpFileComparison.getExpectedFile(): File {
        val homeDir = KtTestUtil.getHomeDirectory()
        return File(homeDir, outputFilePath)
    }

    abstract fun SourceDirectory.ForDumpFileComparison.getErrorMessage(): String
    abstract fun PsiFile.processFile(): List<String>
}


/**
 * Test for checking the code base without building the resulting text.
 *
 * Such a test can be used for properties that are not allowed to be violated.
 * The test is expected to directly throw any custom exceptions from [processFile] in case of violations.
 */
abstract class AbstractAnalysisApiCodebaseValidationTest :
    AbstractAnalysisApiCodebaseTest<SourceDirectory.ForValidation>() {
    override fun SourceDirectory.ForValidation.traverse(psiManager: PsiManager, fileSystem: VirtualFileSystem) {
        val roots = getRoots()

        for (root in roots) {
            for (file in root.walkTopDown()) {
                val psiFile = file.getPsiFile(psiManager, fileSystem) ?: continue
                processFile(file, psiFile)
            }
        }
    }

    abstract fun processFile(file: File, psiFile: PsiFile)
}
