/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.LightStubBuilder
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.PackagePartClassUtils
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubKindImpl
import org.jetbrains.kotlin.psi.stubs.light.firstChildOfType
import org.jetbrains.kotlin.psi.stubs.light.firstExpressionChild
import org.jetbrains.kotlin.psi.stubs.light.hasModifier
import org.jetbrains.kotlin.psi.stubs.light.nodeText
import org.jetbrains.kotlin.psi.stubs.light.packageFqName
import org.jetbrains.kotlin.psi.stubs.light.userTypeReferencedName

/**
 * Builds Kotlin stubs from a [LighterAST]. During indexing the tree comes directly from the parser without
 * materializing the PSI tree (see [org.jetbrains.kotlin.psi.stubs.KotlinLanguageStubDefinition.parseContentsLight]);
 * for loaded files it is a view over the AST. Both must produce stubs identical to the ones the AST-based
 * [KtFileStubBuilder] builds.
 */
internal class KtFileLightStubBuilder : LightStubBuilder() {
    override fun buildStubTree(file: PsiFile): StubElement<*> {
        (file as? KtFile)?.customStubBuilder?.let {
            return it.buildStubTree(file)
        }

        // When the AST is already materialized, the light tree is only a view over it, and walking the view is
        // slower than walking the AST directly, so the AST-based builder is used. Both builders produce identical
        // stubs. A forced light tree set by the platform (see `StubTreeBuilder`) must be consumed by the light path.
        if (FORCED_AST.get() == null) {
            val treeElement = (file as? PsiFileImpl)?.treeElement
            if (treeElement != null && treeElement.isParsed) {
                return KtFileStubBuilder().buildStubTree(file)
            }
        }

        return super.buildStubTree(file)
    }

    override fun createStubForFile(file: PsiFile, tree: LighterAST): StubElement<*> {
        if (file !is KtFile) {
            return super.createStubForFile(file, tree)
        }

        val packageFqName = tree.packageFqName()
        val errorMessage = findErrorMessage(file, tree)

        val kind = when {
            errorMessage != null -> KotlinFileStubKindImpl.Invalid(errorMessage)
            isScript(tree) -> KotlinFileStubKindImpl.Script(packageFqName = packageFqName)
            hasTopLevelCallables(tree) -> KotlinFileStubKindImpl.Facade(
                packageFqName = packageFqName,
                facadeFqName = facadeFqName(file, tree, packageFqName),
            )

            else -> KotlinFileStubKindImpl.File(packageFqName = packageFqName)
        }

        return KotlinFileStubImpl(file, kind)
    }

    /** Mirrors `KtCommonFile.isScript`: a `SCRIPT` node before the first declaration. */
    private fun isScript(tree: LighterAST): Boolean {
        for (child in tree.getChildren(tree.root)) {
            when (child.tokenType) {
                KtNodeTypes.SCRIPT -> return true
                in KtFile.FILE_DECLARATION_TYPES -> return false
                else -> {}
            }
        }

        return false
    }

    /** Mirrors `KtCommonFile.hasTopLevelCallables`. */
    private fun hasTopLevelCallables(tree: LighterAST): Boolean = tree.getChildren(tree.root).any {
        it.tokenType in TOP_LEVEL_CALLABLE_TYPES && !tree.hasModifier(it, KtTokens.EXPECT_KEYWORD)
    }

    /**
     * Mirrors `JvmFileClassUtil.getFileClassInfoNoResolve`'s `facadeClassFqName`: the `@file:JvmName` /
     * `@file:JvmPackageName` annotations may replace the facade simple name and its package.
     */
    private fun facadeFqName(file: KtFile, tree: LighterAST, packageFqName: FqName): FqName {
        val jvmName = findFileAnnotation(tree, JVM_NAME_SHORT)
            ?.let { literalStringArgument(tree, it) }
            ?.takeIf(Name::isValidIdentifier)

        val jvmPackageName = findFileAnnotation(tree, JVM_PACKAGE_NAME_SHORT)
            ?.let { literalStringArgument(tree, it) }
            ?.let(::FqName)

        val facadePackage = jvmPackageName ?: packageFqName
        val simpleName = jvmName ?: PackagePartClassUtils.getFilePartShortName(file)
        return facadePackage.child(Name.identifier(simpleName))
    }

    /**
     * Mirrors `JvmFileClassUtil.findAnnotationEntryOnFileNoResolve`: the first entry of the file annotation list
     * whose callee references [shortName].
     */
    private fun findFileAnnotation(tree: LighterAST, shortName: String): LighterASTNode? {
        val annotationList = tree.firstChildOfType(tree.root, KtNodeTypes.FILE_ANNOTATION_LIST) ?: return null

        // `KtAnnotationsContainer.getAnnotationEntries` collects both direct entries and entries grouped in
        // `@file:[...]` annotations
        val entries = tree.getChildren(annotationList).flatMap {
            when (it.tokenType) {
                KtNodeTypes.ANNOTATION_ENTRY -> listOf(it)
                KtNodeTypes.ANNOTATION -> tree.getChildren(it).filter { child -> child.tokenType == KtNodeTypes.ANNOTATION_ENTRY }
                else -> emptyList()
            }
        }

        return entries.firstOrNull { annotationShortName(tree, it) == shortName }
    }

    /** Mirrors `calleeExpression?.constructorReferenceExpression?.getReferencedName()`. */
    private fun annotationShortName(tree: LighterAST, annotationEntry: LighterASTNode): String? {
        val callee = tree.firstChildOfType(annotationEntry, KtNodeTypes.CONSTRUCTOR_CALLEE) ?: return null
        val typeReference = tree.firstChildOfType(callee, KtNodeTypes.TYPE_REFERENCE) ?: return null
        val userType = tree.firstChildOfType(typeReference, KtTokenSets.TYPE_ELEMENT_TYPES)
            ?.takeIf { it.tokenType == KtNodeTypes.USER_TYPE }
            ?: return null
        return tree.userTypeReferencedName(userType)
    }

    /** Mirrors `JvmFileClassUtil.getLiteralStringFromAnnotation`: the single literal entry of the first argument. */
    private fun literalStringArgument(tree: LighterAST, annotationEntry: LighterASTNode): String? {
        val argumentList = tree.firstChildOfType(annotationEntry, KtNodeTypes.VALUE_ARGUMENT_LIST) ?: return null
        val argument = tree.firstChildOfType(argumentList, KtNodeTypes.VALUE_ARGUMENT) ?: return null
        val stringTemplate = tree.firstExpressionChild(argument)
            ?.takeIf { it.tokenType == KtNodeTypes.STRING_TEMPLATE }
            ?: return null

        val entry = tree.getChildren(stringTemplate)
            .filter { it.tokenType in STRING_TEMPLATE_ENTRY_TYPES }
            .singleOrNull()
            ?.takeIf { it.tokenType == KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY }
            ?: return null

        return tree.nodeText(entry)
    }

    /**
     * Mirrors the `findErrorMessage` check of [KtFileStubBuilder]: a special comment right after the import list of
     * a compiled file marks it as non-decompilable.
     */
    private fun findErrorMessage(file: KtFile, tree: LighterAST): String? {
        if (!file.isCompiled) return null

        val children = tree.getChildren(tree.root)
        val importListIndex = children.indexOfFirst { it.tokenType == KtNodeTypes.IMPORT_LIST }
        if (importListIndex < 0) return null

        val next = children.getOrNull(importListIndex + 1) ?: return null
        if (next.tokenType !in KtTokens.COMMENTS) return null
        if (tree.nodeText(next) != INCOMPATIBLE_ABI_VERSION_COMMENT) return null

        return file.text
    }

    companion object {
        private val TOP_LEVEL_CALLABLE_TYPES = TokenSet.create(
            KtNodeTypes.PROPERTY,
            KtNodeTypes.FUN,
            KtNodeTypes.SCRIPT,
            KtNodeTypes.TYPEALIAS,
        )

        private val STRING_TEMPLATE_ENTRY_TYPES = TokenSet.create(
            KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY,
            KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY,
            KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY,
            KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY,
        )

        private const val JVM_NAME_SHORT = "JvmName"
        private const val JVM_PACKAGE_NAME_SHORT = "JvmPackageName"

        private const val INCOMPATIBLE_ABI_VERSION_COMMENT =
            "// This file was compiled with a newer version of Kotlin compiler and can't be decompiled."
    }
}
