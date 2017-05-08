/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

class ValToObjectIntention : SelfTargetingIntention<KtProperty>(KtProperty::class.java, "Convert to object declaration") {

    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        if (element.isVar) return false
        if (!element.isTopLevel) return false

        val initializer = element.initializer as? KtObjectLiteralExpression ?: return false
        if (initializer.objectDeclaration.getBody() == null) return false

        if (element.getter != null) return false
        if (element.annotationEntries.isNotEmpty()) return false

        // disable if has non-Kotlin usages
        return ReferencesSearch.search(element).all { it is KtReference && it.element.parent !is KtCallableReferenceExpression }
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val modifier = element.visibilityModifier()
        val name = element.name ?: return
        val objectLiteral = element.initializer as? KtObjectLiteralExpression ?: return
        val declaration = objectLiteral.objectDeclaration
        val superTypeList = declaration.getSuperTypeList()
        val body = declaration.getBody() ?: return

        val prefix = modifier?.text?.plus(" ") ?: ""
        val superTypesText = superTypeList?.text?.plus(" ") ?: ""

        val replacementText = "${prefix}object $name: $superTypesText${body.text}"
        val replaced = element.replaced(KtPsiFactory(element).createDeclarationByPattern<KtObjectDeclaration>(replacementText))

        editor?.caretModel?.moveToOffset(replaced.nameIdentifier?.endOffset ?: return)
    }
}