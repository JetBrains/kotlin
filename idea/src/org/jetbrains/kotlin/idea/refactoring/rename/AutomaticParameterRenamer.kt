/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded

class AutomaticParameterRenamer(element: KtParameter, newName: String) : AutomaticRenamer() {
    init {
        processHierarchy(element, newName)
    }

    private fun processHierarchy(element: KtParameter, newName: String) {
        val function = element.ownerFunction ?: return
        for (overrider in HierarchySearchRequest(function, function.useScope).searchOverriders()) {
            val callable = overrider.namedUnwrappedElement ?: continue
            if (!callable.canRefactor()) continue
            val parameter: PsiNamedElement? = when (callable) {
                is KtCallableDeclaration -> callable.valueParameters.firstOrNull { it.name == element.name }
                is PsiMethod -> callable.parameterList.parameters.firstOrNull { it.name == element.name }
                else -> null
            }
            if (parameter == null) continue
            myElements += parameter
        }
        suggestAllNames(element.name, newName.quoteIfNeeded())
    }

    override fun getDialogTitle() = KotlinBundle.message("text.rename.parameters.title")

    override fun getDialogDescription() = RefactoringBundle.message("rename.parameters.hierarchy")

    override fun entityName() = KotlinBundle.message("text.parameter")

    override fun isSelectedByDefault() = true
}

class AutomaticParameterRenamerFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is KtParameter && element.ownerFunction is KtNamedFunction

    override fun getOptionName() = RefactoringBundle.message("rename.parameters.hierarchy")!!

    override fun isEnabled() = KotlinRefactoringSettings.instance.renameParameterInHierarchy

    override fun setEnabled(enabled: Boolean) {
        KotlinRefactoringSettings.instance.renameParameterInHierarchy = enabled
    }

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>): AutomaticRenamer {
        return AutomaticParameterRenamer(element as KtParameter, newName)
    }
}