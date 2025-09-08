/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPublic

/**
 * This test was introduced to automatically check that every public API from some module
 * is documented (i.e., has a KDoc attached).
 *
 * The test iterates through all the source directories [sourceDirectories] and
 * for each directory [SourceDirectory.sourcePaths] builds a separate resulting file
 * containing all the undocumented public declarations along with fully qualified names and parameter types.
 *
 * Then the test compares the contents of the resulting file
 * and the master file [SourceDirectory.ForDumpFileComparison.outputFilePath]
 *
 * The test is intended to prevent developers from writing undocumented APIs.
 * If the lack of documentation for some declaration is intentional,
 * the developer has to manually add this declaration to the master file.
 */
abstract class AbstractKDocCoverageTest : AbstractAnalysisApiCodebaseDumpFileComparisonTest() {
    override fun PsiFile.processFile(): List<String> = buildList {
        when (this@processFile) {
            is KtFile if packageFqName !in ignoredPackages -> addAll(
                getUndocumentedDeclarationsByFile(this@processFile)
            )
            is PsiJavaFile if packageName !in ignoredPackages.map { it.toString() } -> addAll(
                getUndocumentedDeclarationsByFile(this@processFile)
            )
        }
    }

    override fun SourceDirectory.ForDumpFileComparison.getErrorMessage(): String =
        """
            The list of public undocumented declarations in `${getRoots()}` does not match the expected list in `${outputFilePath}`.
            If you added new undocumented declarations, please document them.
            Otherwise, update the exclusion list accordingly.
        """.trimIndent()

    private fun getUndocumentedDeclarationsByFile(file: KtFile): List<String> =
        file.collectPublicDeclarations()
            .filter { it.shouldBeRendered() }
            .map { it.renderDeclaration() }

    private fun getUndocumentedDeclarationsByFile(file: PsiJavaFile): List<String> =
        file.collectPublicDeclarations()
            .filter { it.shouldBeRendered() }
            .map { it.renderDeclaration() }


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

    protected open val ignoredPropertyNames: List<String> = listOf()

    protected open val ignoredFunctionNames: List<String> = listOf()

    protected open val ignoredPackages: List<FqName> = listOf()
}