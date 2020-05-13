/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.Name

abstract class KDocReference(element: KDocName) : KtMultiReference<KDocName>(element) {
    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    override fun canRename(): Boolean = true

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    abstract override fun handleElementRename(newElementName: String): PsiElement?

    override fun getCanonicalText(): String = element.getNameText()

    override val resolvesByNames: Collection<Name> get() = listOf(Name.identifier(element.getNameText()))
}
