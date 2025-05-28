/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.api

import com.intellij.psi.PsiDocCommentBase
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag

// Don't implement KtElement (or it will be treated as statement)
interface KDoc : PsiDocCommentBase, KDocElement {
    override fun getOwner(): KtDeclaration?
    fun getDefaultSection(): KDocSection
    fun getAllSections(): List<KDocSection>
    fun findSectionByName(name: String): KDocSection?
    fun findSectionByTag(tag: KDocKnownTag): KDocSection?
    fun findSectionByTag(tag: KDocKnownTag, subjectName: String): KDocSection?
}
