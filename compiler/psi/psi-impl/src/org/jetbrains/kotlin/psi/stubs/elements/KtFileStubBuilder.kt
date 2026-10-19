/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LazyParseableElement
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.JvmFileClassUtil
import org.jetbrains.kotlin.psi.stubs.StubBuildingLexing
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubKindImpl

class KtFileStubBuilder : DefaultStubBuilder() {
    override fun buildStubTree(file: PsiFile): StubElement<*> {
        (file as? KtFile)?.customStubBuilder?.let {
            return it.buildStubTree(file)
        }

        return super.buildStubTree(file)
    }

    /**
     * Skips not yet parsed code blocks and lambdas which cannot contain stubbed declarations, so they are not parsed only to be traversed.
     *
     * Skipping a node without stubbed declarations doesn't change the stub tree, so the result may differ between building the stub tree
     * and binding it to the AST. Hence, already parsed nodes are just traversed without lexing them once again.
     */
    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean {
        if (node !is LazyParseableElement || node.isParsed) return false

        val elementType = node.elementType
        val isCodeBlock = elementType == KtNodeTypes.LAMBDA_EXPRESSION ||
                // Script blocks contain stubbed top-level declarations
                elementType == KtNodeTypes.BLOCK && parent.elementType != KtNodeTypes.SCRIPT
        if (!isCodeBlock) return false

        (parent.psi?.containingFile as? KtFile)?.customStubBuilder?.let {
            return it.skipChildProcessingWhenBuildingStubs(parent, node)
        }

        return !StubBuildingLexing.mayContainStubbedDeclarations(node.chars)
    }

    override fun createStubForFile(file: PsiFile): StubElement<*> {
        if (file !is KtFile) {
            return super.createStubForFile(file)
        }

        val packageFqName = file.packageFqName
        val errorMessage = findErrorMessage(file)

        val kind = when {
            errorMessage != null -> KotlinFileStubKindImpl.Invalid(errorMessage)
            file.isScript() -> KotlinFileStubKindImpl.Script(packageFqName = packageFqName)
            file.hasTopLevelCallables() -> {
                val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file)
                KotlinFileStubKindImpl.Facade(
                    packageFqName = packageFqName,
                    facadeFqName = fileClassInfo.facadeClassFqName,
                )
            }

            else -> KotlinFileStubKindImpl.File(packageFqName = packageFqName)
        }

        return KotlinFileStubImpl(file, kind)
    }
}

/**
 * Searches for a special error message from [KotlinFileStubKindImpl.Invalid].
 *
 * For now this place is aligned only with the decompiler.
 */
private fun findErrorMessage(file: KtFile): String? {
    if (!file.isCompiled) return null
    val firstComment = file.importList?.nextSibling as? PsiComment ?: return null
    if (firstComment.textMatches("// This file was compiled with a newer version of Kotlin compiler and can't be decompiled.")) {
        return file.text
    }

    return null
}
