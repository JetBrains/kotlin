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
import com.intellij.codeInspection.ex.EntryPointsManager
import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.effectiveVisibility
import org.jetbrains.kotlin.idea.core.isInheritable
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.refactoring.isConstructorDeclaredProperty
import org.jetbrains.kotlin.idea.search.isCheapEnoughToSearchConsideringOperators
import org.jetbrains.kotlin.idea.search.usagesSearch.dataClassComponentFunction
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.isEffectivelyActual
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation

class MemberVisibilityCanBePrivateInspection : AbstractKotlinInspection() {

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
                if (parameter.isConstructorDeclaredProperty() && canBePrivate(parameter)) {
                    registerProblem(holder, parameter)
                }
            }
        }
    }

    private fun canBePrivate(declaration: KtNamedDeclaration): Boolean {
        if (declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) || declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return false
        if (declaration.hasActualModifier() || declaration.isExpectDeclaration()) return false

        val descriptor = (declaration.toDescriptor() as? DeclarationDescriptorWithVisibility) ?: return false
        when (descriptor.effectiveVisibility()) {
            EffectiveVisibility.Private, EffectiveVisibility.Local -> return false
        }

        val classOrObject = declaration.containingClassOrObject ?: return false
        if (classOrObject.isAnnotation()) return false

        val inheritable = classOrObject is KtClass && classOrObject.isInheritable()
        if (!inheritable && declaration.hasModifier(KtTokens.PROTECTED_KEYWORD)) return false //reported by ProtectedInFinalInspection
        if (declaration.isOverridable()) return false

        if (descriptor.hasJvmFieldAnnotation()) return false
        val entryPointsManager = EntryPointsManager.getInstance(declaration.project) as EntryPointsManagerBase
        if (UnusedSymbolInspection.checkAnnotatedUsingPatterns(
                declaration,
                with(entryPointsManager) {
                    additionalAnnotations + ADDITIONAL_ANNOTATIONS
                }
            )
        ) return false

        // properties can be referred by component1/component2, which is too expensive to search, don't analyze them
        if (declaration is KtParameter && declaration.dataClassComponentFunction() != null) return false

        val psiSearchHelper = PsiSearchHelper.SERVICE.getInstance(declaration.project)
        val useScope = declaration.useScope
        val name = declaration.name ?: return false
        val restrictedScope = if (useScope is GlobalSearchScope) {
            when (psiSearchHelper.isCheapEnoughToSearchConsideringOperators(name, useScope, null, null)) {
                PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES -> return false
                PsiSearchHelper.SearchCostResult.ZERO_OCCURRENCES -> return false
                PsiSearchHelper.SearchCostResult.FEW_OCCURRENCES -> KotlinSourceFilterScope.projectSources(useScope, declaration.project)
            }
        } else useScope

        var otherUsageFound = false
        var inClassUsageFound = false
        ReferencesSearch.search(declaration, restrictedScope).forEach(Processor<PsiReference> {
            val usage = it.element
            if (classOrObject != usage.getParentOfType<KtClassOrObject>(false)) {
                otherUsageFound = true
                false
            } else {
                val function = usage.getParentOfType<KtCallableDeclaration>(false)
                val insideInlineFun = function?.modifierList?.let {
                    it.hasModifier(KtTokens.INLINE_KEYWORD) && !function.isPrivate()
                } ?: false
                if (insideInlineFun) {
                    otherUsageFound = true
                    false
                } else {
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
        holder.registerProblem(
            declaration.visibilityModifier() ?: nameElement,
            "$member '${declaration.getName()}' can be private",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            IntentionWrapper(AddModifierFix(modifierListOwner, KtTokens.PRIVATE_KEYWORD), declaration.containingKtFile)
        )
    }
}
