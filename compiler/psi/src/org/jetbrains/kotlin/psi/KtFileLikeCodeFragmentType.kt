/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParser

class KtFileLikeCodeFragmentType : ICodeFragmentElementType(NAME, KotlinLanguage.INSTANCE) {
    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? {
        val project = psi.project
        val languageForParser = getLanguageForParser(psi)
        val builder =
            PsiBuilderFactory.getInstance().createBuilder(project, chameleon, /* lexer = */null, languageForParser, chameleon.chars)
        return KotlinParser.parseFileLikeCodeFragment(builder).firstChildNode
    }

    companion object {
        private const val NAME = "kotlin.FILE_LIKE_CODE_FRAGMENT"
    }
}

