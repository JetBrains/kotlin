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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.refactoring.isConstructorDeclaredProperty
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class MemberVisibilityCanPrivateInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                if (!property.isLocal && canBePrivate(property)) {
                    registerProblem(holder, property)
                }
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                if (canBePrivate(function)) {
                    registerProblem(holder, function)
                }
            }

            override fun visitParameter(parameter: KtParameter) {
                super.visitParameter(parameter)
                if (parameter.isConstructorDeclaredProperty()) {
                    if ((parameter.ownerFunction?.parent as? KtClass)?.isPrivate() == true) return
                    if (canBePrivate(parameter)) registerProblem(holder, parameter)
                }
            }
        }
    }


    private fun canBePrivate(declaration: KtNamedDeclaration): Boolean {
        if (declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) || declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false
        val classOrObject = declaration.containingClassOrObject ?: return false
        val inheritable = classOrObject is KtClass && classOrObject.isInheritable()
        if (!inheritable && declaration.hasModifier(KtTokens.PROTECTED_KEYWORD)) return false //reported by ProtectedInFinalInspection
        if (declaration.isOverridable()) return false

        val psiSearchHelper = PsiSearchHelper.SERVICE.getInstance(declaration.project)
        val useScope = declaration.useScope
        val name = declaration.name ?: return false
        if (useScope is GlobalSearchScope) {
            when (psiSearchHelper.isCheapEnoughToSearch(name, useScope, null, null)) {
                PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES -> return false
                PsiSearchHelper.SearchCostResult.ZERO_OCCURRENCES -> return false
                PsiSearchHelper.SearchCostResult.FEW_OCCURRENCES -> {
                }
            }
        }

        var otherUsageFound = false
        var inClassUsageFound = false
        ReferencesSearch.search(declaration, useScope).forEach(Processor<PsiReference> {
            val usage = it.element
            if (classOrObject != usage.getParentOfType<KtClassOrObject>(false)) {
                otherUsageFound = true
                false
            }
            else {
                val function = usage.getParentOfType<KtCallableDeclaration>(false)
                val insideInlineFun = function?.modifierList?.let {
                    it.hasModifier(KtTokens.INLINE_KEYWORD) && !function.isPrivate()
                } ?: false
                if (insideInlineFun) {
                    otherUsageFound = true
                    false
                }
                else {
                    inClassUsageFound = true
                    true
                }
            }
        })
        return inClassUsageFound && !otherUsageFound
    }

    private fun registerProblem(holder: ProblemsHolder, declaration: KtDeclaration) {
        val modifierListOwner = declaration.getParentOfType<KtModifierListOwner>(false) ?: return
        val member = when (declaration) {
            is KtNamedFunction -> "Function"
            else -> "Property"
        }
        val nameElement = (declaration as? PsiNameIdentifierOwner)?.nameIdentifier ?: return
        holder.registerProblem(declaration.visibilityModifier() ?: nameElement,
                               "$member '${declaration.name}' can be private",
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                               IntentionWrapper(AddModifierFix(modifierListOwner, KtTokens.PRIVATE_KEYWORD), declaration.containingFile))
    }
}