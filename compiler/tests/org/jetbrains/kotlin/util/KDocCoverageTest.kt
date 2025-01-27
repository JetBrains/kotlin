/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.childrenOfType
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

/**
 * This test was introduced to automatically check that every public API from some module
 * is documented (i.e., has a KDoc attached).
 *
 * The test iterates through all the source directories [sourceDirectories] and
 * for each directory [SourceDirectoryWithOutput.sourceCodeDirectoryPath] builds a separate resulting file
 * containing all the undocumented public declarations along with
 * file paths, names of containing classes, annotations, keywords and signatures.
 *
 * Then the test compares the contents of the resulting file
 * and the master file [SourceDirectoryWithOutput.outputFilePath]
 *
 * The test is intended to prevent developers from writing undocumented APIs.
 * If the lack of documentation for some declaration is intentional,
 * the developer has to manually add this declaration to the master file.
 */
abstract class KDocCoverageTest : AbstractRawFirBuilderTestCase() {
    protected fun doTest() {
        sourceDirectories.forEach { (sourceCodeDirectoryPath, outputFilePath) ->
            val actualText = buildList {
                val path = testDataPath + sourceCodeDirectoryPath
                val root = File(path)

                for (file in root.walkTopDown()) {
                    if (file.isDirectory) continue
                    if (file.extension != "kt" && file.extension != "java") continue

                    val relativePath = file.relativeTo(root).invariantSeparatorsPath

                    try {
                        val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
                        val file = getPsiFile(text, file.path)
                        if (file.packageFqName in ignoredPackages)
                            continue
                        addAll(getUndocumentedDeclarationsByFile(file, relativePath))
                    } catch (e: Exception) {
                        throw IllegalStateException(relativePath, e)
                    }
                }
            }.sorted().joinToString("\n")

            val expectedFile = File(testDataPath + outputFilePath)
            KotlinTestUtils.assertEqualsToFile(
                "Some newer public declarations from `$sourceCodeDirectoryPath` are undocumented. Please, consider either documenting them or adding them to `$outputFilePath`",
                expectedFile,
                actualText
            )
        }
    }

    private fun getUndocumentedDeclarationsByFile(file: KtFile, relativePathFromRoot: String): List<String> =
        file.collectPublicDeclarations()
            .filter { it.shouldBeRendered() }
            .map {
                "$relativePathFromRoot:${it.containingClassOrObject?.name?.plus(":") ?: ""}${it.getSignature()}"
            }


    private fun KtDeclaration.getSignature(): String {
        val modifierList =
            this.childrenOfType<KtModifierList>()
                .singleOrNull()
                ?.allChildren
                ?.filter { it !is PsiWhiteSpace && it.text !in nonRenderedModifiers }
                ?.joinToString(" ") { it.text }.orEmpty().let {
                    if (it.isNotEmpty()) "$it " else ""
                }

        val parameterList =
            this.childrenOfType<KtParameterList>().singleOrNull()
                ?.allChildren
                ?.filterIsInstance<KtParameter>()?.joinToString(", ") {
                    it.typeReference?.text ?: ""
                }?.let {
                    "($it)"
                } ?: ""

        return "$modifierList$name$parameterList"
    }

    private fun KtFile.collectPublicDeclarations(): List<KtDeclaration> = buildList {
        this@collectPublicDeclarations.declarations.forEach {
            this.collectPublicNestedDeclarations(it)
        }
    }

    private fun MutableList<KtDeclaration>.collectPublicNestedDeclarations(declaration: KtDeclaration) {
        if (!declaration.isPublic) return

        add(declaration)
        (declaration as? KtDeclarationContainer)?.declarations?.forEach { collectPublicNestedDeclarations(it) }
    }

    private fun KtDeclaration.shouldBeRendered(): Boolean =
        when {
            (this as? KtObjectDeclaration)?.isCompanion() == true -> false
            this.hasModifier(KtTokens.OVERRIDE_KEYWORD) -> false
            this is KtProperty && this.name in ignoredPropertyNames -> false
            this is KtNamedFunction && this.name in ignoredFunctionNames -> false
            else -> this.docComment == null
        }

    private fun getPsiFile(text: String, path: String): KtFile {
        return createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(path)), text) as KtFile
    }

    abstract val sourceDirectories: List<SourceDirectoryWithOutput>

    protected open val ignoredPropertyNames: List<String> = listOf()

    protected open val ignoredFunctionNames: List<String> = listOf()

    protected open val ignoredPackages: List<FqName> = listOf()

    protected open val nonRenderedModifiers: List<String> = listOf(
        "public"
    )

    data class SourceDirectoryWithOutput(
        val sourceCodeDirectoryPath: String,
        val outputFilePath: String,
    )
}