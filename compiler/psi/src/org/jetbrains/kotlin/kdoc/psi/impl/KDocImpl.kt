/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.impl

import com.intellij.lang.Language
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag

class KDocImpl(buffer: CharSequence?) : LazyParseablePsiElement(KDocTokens.KDOC, buffer), KDoc {

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun toString(): String = node.elementType.toString()

    override fun getTokenType(): IElementType = KtTokens.DOC_COMMENT

    override fun getOwner(): KtDeclaration? = getParentOfType(true)

    override fun getDefaultSection(): KDocSection = getChildOfType()!!

    override fun getAllSections(): List<KDocSection> =
        getChildrenOfType<KDocSection>().toList()

    override fun findSectionByName(name: String): KDocSection? =
        getChildrenOfType<KDocSection>().firstOrNull { it.name == name }

    override fun findSectionByTag(tag: KDocKnownTag): KDocSection? =
        findSectionByName(tag.name.toLowerCase())

    override fun findSectionByTag(tag: KDocKnownTag, subjectName: String): KDocSection? =
        getChildrenOfType<KDocSection>().firstOrNull {
            it.name == tag.name.toLowerCase() && it.getSubjectName() == subjectName
        }
}
