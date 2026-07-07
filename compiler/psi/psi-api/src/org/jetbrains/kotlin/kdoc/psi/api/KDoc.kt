/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.api

import com.intellij.psi.PsiDocCommentBase
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag

/**
 * Represents a KDoc documentation comment attached to a declaration.
 *
 * A KDoc is split into sections: a default (primary) section holding the main description, plus one section per block
 * tag (such as `@param` or `@return`). Use [getAllSections] to list them, or [findSectionByName] /
 * [findSectionByTag] to look one up.
 */
interface KDoc : PsiDocCommentBase, KDocElement {
    override fun getOwner(): KtDeclaration?

    /** Returns the default (primary) section — the description that precedes any block tags. */
    fun getDefaultSection(): KDocSection

    /** Returns all sections of this KDoc, starting with the [default section][getDefaultSection]. */
    fun getAllSections(): List<KDocSection>

    /** Returns the section with the given [name], or `null` if this KDoc has no such section. */
    fun findSectionByName(name: String): KDocSection?

    /** Returns the first section for the given block [tag], or `null` if there is none. */
    fun findSectionByTag(tag: KDocKnownTag): KDocSection?

    /** Returns the section for the given block [tag] with the given [subjectName], or `null` if there is none. */
    fun findSectionByTag(tag: KDocKnownTag, subjectName: String): KDocSection?
}
