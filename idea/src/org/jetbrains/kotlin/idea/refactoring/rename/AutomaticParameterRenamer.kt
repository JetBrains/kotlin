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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
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
            val parameter: PsiNamedElement? =
                    when (callable) {
                        is KtCallableDeclaration -> callable.valueParameters.firstOrNull { it.name == element.name }
                        is PsiMethod -> callable.parameterList.parameters.firstOrNull { it.name == element.name }
                        else -> null
                    }
            if (parameter == null) continue
            myElements += parameter
        }
        suggestAllNames(element.name, newName.quoteIfNeeded())
    }

    override fun getDialogTitle() = "Rename Parameters"

    override fun getDialogDescription() = "Rename parameter in hierarchy to:"

    override fun entityName() = "Parameter"

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