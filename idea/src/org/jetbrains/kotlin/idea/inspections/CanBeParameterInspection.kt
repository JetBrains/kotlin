/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.FEW_OCCURRENCES
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.ZERO_OCCURRENCES
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.quickfix.RemoveValVarFromParameterFix
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.isCheapEnoughToSearchConsideringOperators
import org.jetbrains.kotlin.idea.search.usagesSearch.getAccessorNames
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.compat.psiSearchHelperInstance
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

internal val CONSTRUCTOR_VAL_VAR_MODIFIERS = listOf(
    OPEN_KEYWORD, FINAL_KEYWORD, OVERRIDE_KEYWORD,
    PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD,
    LATEINIT_KEYWORD
)

class CanBeParameterInspection : AbstractKotlinInspection() {
    private fun PsiReference.usedAsPropertyIn(klass: KtClass): Boolean {
        if (this !is KtSimpleNameReference) return true
        val nameExpression = element
        // receiver.x
        val parent = element.parent
        if (parent is KtQualifiedExpression) {
            if (parent.selectorExpression == element) return true
        }
        // x += something
        if (parent is KtBinaryExpression &&
            parent.left == element &&
            KtPsiUtil.isAssignment(parent)
        ) return true
        // init / constructor / non-local property?
        var parameterUser: PsiElement = nameExpression
        do {
            parameterUser = PsiTreeUtil.getParentOfType(
                parameterUser, KtProperty::class.java, KtPropertyAccessor::class.java,
                KtClassInitializer::class.java,
                KtFunction::class.java, KtObjectDeclaration::class.java,
                KtSuperTypeCallEntry::class.java
            ) ?: return true
        } while (parameterUser is KtProperty && parameterUser.isLocal)
        return when (parameterUser) {
            is KtProperty -> parameterUser.containingClassOrObject !== klass
            is KtClassInitializer -> parameterUser.containingDeclaration !== klass
            is KtFunction, is KtObjectDeclaration, is KtPropertyAccessor -> true
            is KtSuperTypeCallEntry -> parameterUser.getStrictParentOfType<KtClassOrObject>() !== klass
            else -> true
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return parameterVisitor(fun(parameter) {
            // Applicable to val / var parameters of a class / object primary constructors
            val valOrVar = parameter.valOrVarKeyword ?: return
            val name = parameter.name ?: return
            if (parameter.hasModifier(OVERRIDE_KEYWORD)) return
            if (parameter.annotationEntries.isNotEmpty()) return
            val constructor = parameter.parent.parent as? KtPrimaryConstructor ?: return
            val klass = constructor.getContainingClassOrObject() as? KtClass ?: return
            if (klass.isData()) return

            val useScope = parameter.useScope
            val restrictedScope = if (useScope is GlobalSearchScope) {
                val psiSearchHelper = psiSearchHelperInstance(parameter.project)
                for (accessorName in parameter.getAccessorNames()) {
                    when (psiSearchHelper.isCheapEnoughToSearchConsideringOperators(accessorName, useScope, null, null)) {
                        ZERO_OCCURRENCES -> {
                        } // go on
                        else -> return         // accessor in use: should remain a property
                    }
                }
                // TOO_MANY_OCCURRENCES: too expensive
                // ZERO_OCCURRENCES: unused at all, reported elsewhere
                if (psiSearchHelper.isCheapEnoughToSearchConsideringOperators(name, useScope, null, null) != FEW_OCCURRENCES) return
                KotlinSourceFilterScope.projectSources(useScope, parameter.project)
            } else useScope
            // Find all references and check them
            val references = ReferencesSearch.search(parameter, restrictedScope)
            if (references.none()) return
            if (references.any { it.usedAsPropertyIn(klass) }) return
            holder.registerProblem(
                valOrVar,
                "Constructor parameter is never used as a property",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                RemoveValVarFix(parameter)
            )
        })
    }

    class RemoveValVarFix(parameter: KtParameter) : LocalQuickFix {

        private val fix = RemoveValVarFromParameterFix(parameter)

        override fun getName() = fix.text

        override fun getFamilyName() = fix.familyName

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val parameter = descriptor.psiElement.getParentOfType<KtParameter>(strict = true) ?: return
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            parameter.valOrVarKeyword?.delete()
            // Delete visibility / open / final / lateinit, if any
            // Retain annotations / vararg
            // override should never be here
            val modifierList = parameter.modifierList ?: return
            for (modifier in CONSTRUCTOR_VAL_VAR_MODIFIERS) {
                modifierList.getModifier(modifier)?.delete()
            }
        }
    }
}

