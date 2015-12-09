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
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

class ConvertPropertyInitializerToGetterIntention : SelfTargetingIntention<KtProperty>(javaClass(), "Convert property initializer to getter") {
    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        return element.initializer != null
               && element.initializer?.textRange?.containsOffset(caretOffset) == true
               && element.getter == null
               && !element.isExtensionDeclaration()
               && !element.isLocal
    }

    override fun applyTo(property: KtProperty, editor: Editor) {
        convertPropertyInitializerToGetter(property, editor)
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
