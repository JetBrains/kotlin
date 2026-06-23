/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParser

/**
 * The file element type for Kotlin files.
 *
 * Stub support is decoupled from this element type (KT-78356): the file stub is built by
 * [org.jetbrains.kotlin.psi.stubs.registry.KotlinLanguageStubDefinition] and serialized by
 * [org.jetbrains.kotlin.psi.stubs.factory.KotlinFileStubSerializer], both registered via the platform stub registry.
 */
object KtFileElementType : IFileElementType(KtFileElementType.NAME, KotlinLanguage.INSTANCE) {
    internal const val NAME = "kotlin.FILE"

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? {
        val project = psi.project
        val languageForParser = getLanguageForParser(psi)
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.chars)
        return KotlinParser.parse(builder, psi.containingFile).firstChildNode
    }
}
