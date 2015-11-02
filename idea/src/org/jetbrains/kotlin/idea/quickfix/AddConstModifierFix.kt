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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ConstModifierChecker
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

public class AddConstModifierFix(val property: KtProperty) : AddModifierFix(property, KtTokens.CONST_KEYWORD), CleanupFix {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        addConstModifier(property)
    }

    companion object {
        fun addConstModifier(property: KtProperty) {
            replaceReferencesToGetterByReferenceToField(property)
            property.addModifier(KtTokens.CONST_KEYWORD)
        }
    }
}

public class AddConstModifierIntention : SelfTargetingIntention<KtProperty>(javaClass(), "Add 'const' modifier") {
    override fun applyTo(element: KtProperty, editor: Editor) {
        AddConstModifierFix.addConstModifier(element)
    }

    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        return isApplicableTo(element)
    }

    companion object {
        fun isApplicableTo(element: KtProperty): Boolean {
            if (element.isLocal || element.isVar || element.hasDelegate() || element.initializer == null
                    || element.getter?.hasBody() == true || element.receiverTypeReference != null) {
                return false
            }
            val propertyDescriptor = element.descriptor as? VariableDescriptor ?: return false
            return ConstModifierChecker.checkCanBeConst(element, element, propertyDescriptor) == null
        }
    }
}


public object ConstFixFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val expr = diagnostic.psiElement as? KtReferenceExpression ?: return null
        val bindingContext = expr.analyze(BodyResolveMode.PARTIAL)
        val targetDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expr) as? VariableDescriptor ?: return null
        val declaration = (targetDescriptor.source as? PsiSourceElement)?.psi as? KtProperty ?: return null
        if (ConstModifierChecker.checkCanBeConst(declaration, declaration, targetDescriptor) == null) {
            return AddConstModifierFix(declaration)
        }
        return null
    }
}

fun replaceReferencesToGetterByReferenceToField(property: KtProperty) {
    val project = property.project
    val getter = LightClassUtil.getLightClassPropertyMethods(property).getter

    val javaScope = GlobalSearchScope.getScopeRestrictedByFileTypes(project.allScope(), JavaFileType.INSTANCE)
    val getterUsages = if (getter != null)
        ReferencesSearch.search(getter, javaScope).findAll()
    else
        emptyList()

    val backingField = LightClassUtil.getLightClassPropertyMethods(property).backingField
    if (backingField != null) {
        val factory = PsiElementFactory.SERVICE.getInstance(project)
        val fieldFQName = backingField.containingClass!!.qualifiedName + "." + backingField.name

        getterUsages.forEach {
            val call = it.element.getNonStrictParentOfType<PsiMethodCallExpression>()
            if (call != null && it.element == call.methodExpression) {
                val fieldRef = factory.createExpressionFromText(fieldFQName, it.element)
                call.replace(fieldRef)
            }
        }
    }
}

