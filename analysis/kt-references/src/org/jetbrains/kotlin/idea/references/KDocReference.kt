/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

abstract class KDocReference(element: KDocName) : KtMultiReference<KDocName>(element) {
    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    override fun canRename(): Boolean = true

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    override fun getCanonicalText(): String = element.getNameText()

    override val resolvesByNames: Collection<Name>
        get() {
            val element = element
            val name = element.getNameText()

            // Text check is required to distinguish between '`this`'/'`super`' and 'this'/'super' cases
            if (name in FORBIDDEN_NAMES && element.textMatches(name)) {
                // According to the KDoc, `this`/`super` cannot be properly expressed in terms of this API
                return emptyList()
            }

            return listOfNotNull(
                Name.identifier(name),
                // A property might resolve into a getter function
                JvmAbi.getterName(name).takeIf { it != name }?.let(Name::identifier),
            )
        }
}

private val FORBIDDEN_NAMES = listOf(KtTokens.THIS_KEYWORD.value, KtTokens.SUPER_KEYWORD.value)
