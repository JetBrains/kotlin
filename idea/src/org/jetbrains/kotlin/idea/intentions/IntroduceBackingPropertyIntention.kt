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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class IntroduceBackingPropertyIntention(): JetSelfTargetingIntention<JetProperty>(javaClass(), "Introduce backing property") {
    override fun isApplicableTo(element: JetProperty, caretOffset: Int): Boolean {
        if (!canIntroduceBackingProperty(element)) return false
        return element.nameIdentifier?.textRange?.containsOffset(caretOffset) == true
    }

    override fun applyTo(element: JetProperty, editor: Editor) {
        introduceBackingProperty(element)
    }

    companion object {
        fun canIntroduceBackingProperty(property: JetProperty): Boolean {
            val name = property.name ?: return false

            val bindingContext = property.getResolutionFacade().analyzeFullyAndGetResult(listOf(property)).bindingContext
            val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property) as? PropertyDescriptor ?: return false
            if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) == false) return false

            val containingClass = property.getStrictParentOfType<JetClassOrObject>() ?: return false
            return containingClass.declarations.none { it is JetProperty && it.name == "_" + name }
        }

        fun introduceBackingProperty(property: JetProperty) {
            createBackingProperty(property)

            if (property.typeReference == null) {
                val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(property)
                SpecifyTypeExplicitlyIntention.addTypeAnnotation(null, property, type)
            }

            val getter = property.getter
            if (getter == null) {
                createGetter(property)
            }
            else {
                replaceFieldReferences(getter, property.name!!)
            }

            if (property.isVar) {
                val setter = property.setter
                if (setter == null) {
                    createSetter(property)
                }
                else {
                    replaceFieldReferences(setter, property.name!!)
                }
            }

            property.setInitializer(null)

            replaceBackingFieldReferences(property)
        }

        private fun createGetter(element: JetProperty) {
            val body = "get() = _${element.name}"
            val newGetter = JetPsiFactory(element).createProperty("val x $body").getter!!
            element.add(newGetter)
        }

        private fun createSetter(element: JetProperty) {
            val body = "set(value) { _${element.name} = value }"
            val newSetter = JetPsiFactory(element).createProperty("val x $body").setter!!
            element.add(newSetter)
        }

        private fun createBackingProperty(property: JetProperty) {
            val backingProperty = JetPsiFactory(property).buildDeclaration {
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

        private fun replaceFieldReferences(element: JetElement, propertyName: String) {
            element.acceptChildren(object : JetTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    val bindingContext = expression.analyze()
                    val target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)
                    if (target is SyntheticFieldDescriptor) {
                        expression.replace(JetPsiFactory(element).createSimpleName("_$propertyName"))
                    }
                }

                override fun visitPropertyAccessor(accessor: JetPropertyAccessor) {
                    // don't go into accessors of properties in local classes because 'field' will mean something different in them
                }
            })
        }

        // TODO: drop this when we get rid of backing field syntax
        private fun replaceBackingFieldReferences(prop: JetProperty) {
            val containingClass = prop.getStrictParentOfType<JetClassOrObject>()!!
            ReferencesSearch.search(prop, LocalSearchScope(containingClass)).forEach {
                val element = it.element as? JetNameReferenceExpression
                if (element != null && element.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                    element.replace(JetPsiFactory(element).createSimpleName("_${prop.name}"))
                }
            }
        }
    }
}

class IntroduceBackingPropertyFix(prop: JetProperty): JetIntentionAction<JetProperty>(prop) {
    override fun getText(): String = "Introduce backing property"
    override fun getFamilyName(): String  = getText()

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        IntroduceBackingPropertyIntention.introduceBackingProperty(element)
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val resolveResult = diagnostic.psiElement.reference?.resolve()
            if (resolveResult is JetProperty &&
                IntroduceBackingPropertyIntention.canIntroduceBackingProperty(resolveResult)) {
                return IntroduceBackingPropertyFix(resolveResult)
            }
            return null
        }
    }
}

