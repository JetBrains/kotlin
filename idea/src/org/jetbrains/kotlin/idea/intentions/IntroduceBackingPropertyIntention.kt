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
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.resolve.BindingContext

class IntroduceBackingPropertyIntention(): JetSelfTargetingIntention<JetProperty>(javaClass(), "Introduce backing property") {
    override fun isApplicableTo(element: JetProperty, caretOffset: Int): Boolean {
        if (!canIntroduceBackingProperty(element)) return false
        var elementAtCaret = element.containingFile.findElementAt(caretOffset)
        if (elementAtCaret is PsiWhiteSpace) {
            elementAtCaret = element.containingFile.findElementAt(caretOffset - 1)
        }
        return elementAtCaret == element.nameIdentifier || elementAtCaret == element.valOrVarKeyword
    }

    override fun applyTo(element: JetProperty, editor: Editor) {
        introduceBackingProperty(element)
    }

    companion object {
        public fun canIntroduceBackingProperty(element: JetProperty): Boolean {
            val name = element.name ?: return false
            if (name.startsWith('_')) return false

            if (element.hasDelegate() || element.receiverTypeReference != null) return false

            val containingClass = element.getStrictParentOfType<JetClassOrObject>() ?: return false
            return containingClass.declarations.none { it is JetProperty && it.name == "_" + name }
        }

        fun introduceBackingProperty(element: JetProperty) {
            createBackingProperty(element)

            val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element)
            SpecifyTypeExplicitlyIntention.addTypeAnnotation(null, element, type)

            val getter = element.getter
            if (getter == null) {
                createGetter(element)
            }
            else {
                replaceFieldReferences(getter, element.name!!)
            }

            if (element.isVar) {
                val setter = element.setter
                if (setter == null) {
                    createSetter(element)
                }
                else {
                    replaceFieldReferences(setter, element.name!!)
                }
            }

            element.removeInitializer()

            replaceBackingFieldReferences(element)
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

        private fun JetProperty.removeInitializer() {
            val initializer = initializer
            if (initializer != null) {
                val eq = initializer.prevLeaf { it.node.elementType == JetTokens.EQ }
                if (eq != null) {
                    initializer.parent.deleteChildRange(eq, initializer)
                }
            }
        }

        private fun createBackingProperty(element: JetProperty) {
            val backingPropertyText = StringBuilder {
                append("private ")
                append(element.valOrVarKeyword.text)
                append(" _")
                append(element.name)
                val typeRef = element.typeReference
                if (typeRef != null) {
                    append(": ")
                    append(typeRef.text)
                }

                val initializer = element.initializer
                if (initializer != null) {
                    append(" = ")
                    append(initializer.text)
                }
            }.toString()

            val backingProp = JetPsiFactory(element).createProperty(backingPropertyText)
            element.parent.addBefore(backingProp, element)
        }

        private fun replaceFieldReferences(element: JetElement, propertyName: String) {
            val bindingContext = element.analyze()
            element.acceptChildren(object : JetTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    val target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)
                    if (target is SyntheticFieldDescriptor) {
                        expression.replace(JetPsiFactory(element).createSimpleName("_$propertyName"))
                    }
                }
            })
        }

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

