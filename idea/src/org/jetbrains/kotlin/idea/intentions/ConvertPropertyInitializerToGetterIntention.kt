/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

class ConvertPropertyInitializerToGetterIntention : SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, "Convert property initializer to getter") {

    override fun applicabilityRange(element: KtProperty): TextRange? {
        val initializer = element.initializer
        if (initializer != null && element.getter == null && !element.isExtensionDeclaration() && !element.isLocal)
            return initializer.textRange
        else
            return null
    }

    override fun allowCaretInsideElement(element: PsiElement) = element !is KtDeclaration // do not work inside lambda's in initializer - they can be too big

    override fun applyTo(element: KtProperty, editor: Editor?) {
        convertPropertyInitializerToGetter(element, editor)
    }

    companion object {
        fun convertPropertyInitializerToGetter(property: KtProperty, editor: Editor?) {
            val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(property)
            SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, property, type)

            val initializer = property.initializer!!
            val getter = KtPsiFactory(property).createPropertyGetter(initializer)
            val setter = property.setter

            if (setter != null) {
                property.addBefore(getter, setter)
            }
            else {
                property.add(getter)
            }

            property.setInitializer(null)
        }
    }
}
