/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.psi.KtPsiMutationService
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * The part of a doc comment which describes a single class, method or property
 * produced by the element being documented. For example, the doc comment of a class
 * can have sections for the class itself, its primary constructor and each of the
 * properties defined in the primary constructor.
 */
class KDocSection : KDocTag, ContributedReferenceHost, PsiLanguageInjectionHost {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    /**
     * Returns the name of the section (the name of the doc tag introducing the section,
     * or null for the default section).
     */
    override fun getName(): String? =
        (firstChild as? KDocTag)?.name

    override fun getSubjectName(): String? =
        (firstChild as? KDocTag)?.getSubjectName()

    override fun getContent(): String =
        (firstChild as? KDocTag)?.getContent() ?: super.getContent()

    /**
     * Returns all tags in this section whose [name][KDocTag.getName] equals the given [name] (without the leading `@`), in source order.
     */
    fun findTagsByName(name: String): List<KDocTag> {
        return getChildrenOfType<KDocTag>().filter { it.name == name }
    }

    /**
     * Returns the first tag in this section whose [name][KDocTag.getName] equals the given [name] (without the leading `@`), or `null` if
     * there is none.
     */
    fun findTagByName(name: String): KDocTag? = findTagsByName(name).firstOrNull()

    override fun getReference(): PsiReference? {
        return references.firstOrNull()
    }

    override fun getReferences(): Array<out PsiReference?> {
        return KotlinReferenceProvidersService.getReferencesFromProviders(this)
    }

    override fun isValidHost(): Boolean = true

    @OptIn(KtNonPublicApi::class)
    override fun updateText(text: String): PsiLanguageInjectionHost =
        KtPsiMutationService.getInstance().updateKDocSectionText(this, text)

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> =
        LiteralTextEscaper.createSimple(this, false)
}
