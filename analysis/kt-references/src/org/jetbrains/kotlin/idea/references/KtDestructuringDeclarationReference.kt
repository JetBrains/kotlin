/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

abstract class KtDestructuringDeclarationReference(
    element: KtDestructuringDeclarationEntry
) : KtMultiReference<KtDestructuringDeclarationEntry>(element) {

    override fun getRangeInElement() = TextRange(0, element.textLength)

    override fun resolve(): PsiElement? = element

    override val resolvesByNames: Collection<Name>
        get() {
            val destructuringParent = element.parent as? KtDestructuringDeclaration ?: return emptyList()
            val componentIndex = destructuringParent.entries.indexOf(element) + 1
            return listOf(Name.identifier("component$componentIndex"))
        }
}
