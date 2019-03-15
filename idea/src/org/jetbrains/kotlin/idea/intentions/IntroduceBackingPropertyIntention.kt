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
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.util.hasJvmFieldAnnotation
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class IntroduceBackingPropertyIntention : SelfTargetingIntention<KtProperty>(KtProperty::class.java, "Introduce backing property") {
    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        if (!canIntroduceBackingProperty(element)) return false
        return element.nameIdentifier?.textRange?.containsOffset(caretOffset) == true
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        introduceBackingProperty(element)
    }

    companion object {
        fun canIntroduceBackingProperty(property: KtProperty): Boolean {
            val name = property.name ?: return false
            if (property.hasModifier(KtTokens.CONST_KEYWORD)) return false
            if (property.hasJvmFieldAnnotation()) return false

            val bindingContext = property.getResolutionFacade().analyzeWithAllCompilerChecks(listOf(property)).bindingContext
            val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property) as? PropertyDescriptor ?: return false
            if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) == false) return false

            val containingClass = property.getStrictParentOfType<KtClassOrObject>() ?: return false
            if (containingClass.isExpectDeclaration()) return false
            return containingClass.declarations.none { it is KtProperty && it.name == "_$name" }
        }

        fun introduceBackingProperty(property: KtProperty) {
            createBackingProperty(property)

            if (property.typeReference == null) {
                val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(property)
                SpecifyTypeExplicitlyIntention.addTypeAnnotation(null, property, type)
            }

            val getter = property.getter
            if (getter == null) {
                createGetter(property)
            } else {
                replaceFieldReferences(getter, property.name!!)
            }

            if (property.isVar) {
                val setter = property.setter
                if (setter == null) {
                    createSetter(property)
                } else {
                    replaceFieldReferences(setter, property.name!!)
                }
            }

            property.initializer = null
        }

        private fun createGetter(element: KtProperty) {
            val body = "get() = _${element.name}"
            val newGetter = KtPsiFactory(element).createProperty("val x $body").getter!!
            element.addAccessor(newGetter)
        }

        private fun createSetter(element: KtProperty) {
            val body = "set(value) { _${element.name} = value }"
            val newSetter = KtPsiFactory(element).createProperty("val x $body").setter!!
            element.addAccessor(newSetter)
        }

        private fun KtProperty.addAccessor(newAccessor: KtPropertyAccessor) {
            val semicolon = node.findChildByType(KtTokens.SEMICOLON)
            addBefore(newAccessor, semicolon?.psi)
        }

        private fun createBackingProperty(property: KtProperty) {
            val backingProperty = KtPsiFactory(property).buildDeclaration {
                appendFixedText("private ")
                appendFixedText(property.valOrVarKeyword.text)
                appendFixedText(" _${property.name}")
                if (property.typeReference != null) {
                    appendFixedText(": ")
                    appendTypeReference(property.typeReference)
                }
                if (property.initializer != null) {
                    appendFixedText(" = ")
                    appendExpression(property.initializer)
                }
            }

            property.parent.addBefore(backingProperty, property)
        }

        private fun replaceFieldReferences(element: KtElement, propertyName: String) {
            element.acceptChildren(object : KtTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                    val target = expression.resolveToCall()?.resultingDescriptor
                    if (target is SyntheticFieldDescriptor) {
                        expression.replace(KtPsiFactory(element).createSimpleName("_$propertyName"))
                    }
                }

                override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                    // don't go into accessors of properties in local classes because 'field' will mean something different in them
                }
            })
        }
    }
}
