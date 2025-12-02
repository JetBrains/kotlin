/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

abstract class KtDestructuringDeclarationReference(
    element: KtDestructuringDeclarationEntry
) : KtMultiReference<KtDestructuringDeclarationEntry>(element) {

    override fun getRangeInElement() = TextRange(0, element.textLength)

    override val resolvesByNames: Collection<Name>
        get() {
            val destructuringParent = element.parent as? KtDestructuringDeclaration ?: return emptyList()

            // In the future, `hasSquareBrackets()` will suffice, but during the migration phase,
            // distinguishing between positional and named destructuring solely through syntax is not possible for the short form
            val isPositionalBased = destructuringParent.hasSquareBrackets() || !destructuringParent.isFullForm
            val isNameBased = !destructuringParent.hasSquareBrackets()
            val propertyUsage = if (isNameBased) {
                element.initializer?.getReferencedNameAsName() ?: element.nameAsName
            } else {
                null
            }

            val componentNUsage = if (isPositionalBased) {
                val componentNIndex = destructuringParent.entries.indexOf(element) + 1
                Name.identifier("component$componentNIndex")
            } else {
                null
            }

            return listOfNotNull(propertyUsage, componentNUsage)
        }
}
