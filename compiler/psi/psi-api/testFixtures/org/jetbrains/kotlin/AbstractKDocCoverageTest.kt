/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

/**
 * This test was introduced to automatically check that every public API from some module
 * is documented (i.e., has a KDoc attached).
 *
 * The test iterates through all the source directories [sourceDirectories] and
 * for each directory [DocumentationLocations.sourcePaths] builds a separate resulting file
 * containing all the undocumented public declarations along with fully qualified names and parameter types.
 *
 * Then the test compares the contents of the resulting file
 * and the master file [DocumentationLocations.outputFilePath]
 *
 * The test is intended to prevent developers from writing undocumented APIs.
 * If the lack of documentation for some declaration is intentional,
 * the developer has to manually add this declaration to the master file.
 */
abstract class AbstractKDocCoverageTest : KtUsefulTestCase() {
    @OptIn(K1Deprecation::class)
    protected fun doTest() {
        val environment = KotlinCoreEnvironment.createForParallelTests(
            testRootDisposable,
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val psiManager = PsiManager.getInstance(environment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val homeDir = KtTestUtil.getHomeDirectory()

        sourceDirectories.forEach { (sourceCodeDirectoryPaths, outputFilePath) ->
            val roots = sourceCodeDirectoryPaths.map { File(homeDir, it) }

            val actualText = buildList {
                for (root in roots) {
                    for (file in root.walkTopDown()) {
                        if (file.isDirectory) continue
                        if (file.extension != "kt" && file.extension != "java") continue

                        val relativePath = file.relativeTo(root).invariantSeparatorsPath

                        try {
                            val psiFile = createPsiFile(file.path, psiManager, fileSystem) ?: continue
                            when (psiFile) {
                                is KtFile if psiFile.packageFqName !in ignoredPackages -> addAll(
                                    getUndocumentedDeclarationsByFile(psiFile)
                                )
                                is PsiJavaFile if psiFile.packageName !in ignoredPackages.map { it.toString() } -> addAll(
                                    getUndocumentedDeclarationsByFile(psiFile)
                                )
                            }
                        } catch (e: Exception) {
                            throw IllegalStateException(relativePath, e)
                        }
                    }
                }
            }.sorted().joinToString("\n")

            val expectedFile = File(homeDir, outputFilePath)
            val errorMessage = """
                The list of public undocumented declarations in `$roots` does not match the expected list in `$outputFilePath`.
                If you added new undocumented declarations, please document them or add them to the exclusion list.
                Otherwise, update the exclusion list accordingly.
            """.trimIndent()
            KotlinTestUtils.assertEqualsToFile(errorMessage, expectedFile, actualText)
        }
    }

    private fun getUndocumentedDeclarationsByFile(file: KtFile): List<String> =
        file.collectPublicDeclarations()
            .filter { it.shouldBeRendered() }
            .map { renderDeclaration(it) }

    private fun getUndocumentedDeclarationsByFile(file: PsiJavaFile): List<String> =
        file.collectPublicDeclarations()
            .filter { it.shouldBeRendered() }
            .map { renderDeclaration(it) }

    private fun renderDeclaration(element: PsiElement): String =
        element.getQualifiedName()?.let { fqn ->
            val parameterList = when (element) {
                is KtDeclaration -> getKtParameterList(element)
                else -> getParameterList(element)
            }
            fqn + parameterList
        } ?: "RENDERING ERROR"

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

    private fun getParameterList(element: PsiElement): String {
        val parameterList =
            element.childrenOfType<PsiParameterList>().singleOrNull()
                ?.allChildren
                ?.filterIsInstance<PsiParameter>()?.joinToString(", ") {
                    it.type.presentableText
                }?.let {
                    "($it)"
                } ?: ""

        return parameterList
    }

    private fun getKtParameterList(declaration: KtDeclaration): String {
        val parameterList =
            declaration.childrenOfType<KtParameterList>().singleOrNull()
                ?.allChildren
                ?.filterIsInstance<KtParameter>()?.joinToString(", ") {
                    it.typeReference?.typeElement?.text ?: ""
                }?.let {
                    "($it)"
                } ?: ""

        return parameterList
    }

    private fun KtFile.collectPublicDeclarations(): List<KtDeclaration> = buildList {
        this@collectPublicDeclarations.declarations.forEach { ktDeclaration ->
            this.collectPublicNestedDeclarations(ktDeclaration)
        }
    }

    private fun MutableList<KtDeclaration>.collectPublicNestedDeclarations(declaration: KtDeclaration) {
        if (!declaration.isPublic) return

        add(declaration)
        (declaration as? KtDeclarationContainer)?.declarations?.forEach { collectPublicNestedDeclarations(it) }
    }


    private fun PsiJavaFile.collectPublicDeclarations(): List<PsiElement> = buildList {
        this@collectPublicDeclarations.classes.forEach { psiClass ->
            this.collectPublicNestedDeclarations(psiClass)
        }
    }

    private fun MutableList<PsiElement>.collectPublicNestedDeclarations(javaClass: PsiClass) {
        if (!javaClass.hasModifier(JvmModifier.PUBLIC)) return

        add(javaClass)
        addAll(javaClass.fields.filter { it.hasModifier(JvmModifier.PUBLIC) })
        addAll(javaClass.methods.filter { it.hasModifier(JvmModifier.PUBLIC) })

        javaClass.innerClasses.forEach { collectPublicNestedDeclarations(it) }
    }

    private fun KtDeclaration.shouldBeRendered(): Boolean =
        when {
            (this as? KtObjectDeclaration)?.isCompanion() == true -> false
            this is KtClassInitializer -> false
            this.hasModifier(KtTokens.OVERRIDE_KEYWORD) -> false
            this is KtProperty && this.name in ignoredPropertyNames -> false
            this is KtNamedFunction && this.name in ignoredFunctionNames -> false
            else -> this.docComment == null
        }

    private fun PsiElement.shouldBeRendered(): Boolean =
        when (this) {
            is PsiModifierListOwner if this.annotations.any { it.text == "@Override" } -> false
            is PsiField if this.name in ignoredPropertyNames -> false
            is PsiMethod if this.name in ignoredFunctionNames -> false
            else -> (this as? PsiDocCommentOwner)?.docComment == null
        }

    abstract val sourceDirectories: List<DocumentationLocations>

    protected open val ignoredPropertyNames: List<String> = listOf()

    protected open val ignoredFunctionNames: List<String> = listOf()

    protected open val ignoredPackages: List<FqName> = listOf()

    data class DocumentationLocations(
        val sourcePaths: List<String>,
        val outputFilePath: String,
    )

    private fun createPsiFile(fileName: String, psiManager: PsiManager, fileSystem: VirtualFileSystem): PsiFile? {
        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")
        val psiFile = psiManager.findFile(file)
        return psiFile
    }
}