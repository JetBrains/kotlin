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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.getAffectedCallables
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.SmartList

class RenameKotlinParameterProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement) = element is KtParameter && element.ownerFunction is KtFunction

    override fun isToSearchInComments(psiElement: PsiElement) = JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_VARIABLE

    override fun setToSearchInComments(element: PsiElement, enabled: Boolean) {
        JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_VARIABLE = enabled
    }

    override fun findCollisions(
            element: PsiElement,
            newName: String?,
            allRenames: MutableMap<out PsiElement, String>,
            result: MutableList<UsageInfo>
    ) {
        if (newName == null) return
        val declaration = element.namedUnwrappedElement as? KtNamedDeclaration ?: return
        val descriptor = declaration.resolveToDescriptor() as VariableDescriptor

        val collisions = SmartList<UsageInfo>()
        checkRedeclarations(descriptor, newName, collisions)
        checkOriginalUsagesRetargeting(declaration, newName, result, collisions)
        checkNewNameUsagesRetargeting(declaration, newName, collisions)
        result += collisions
    }

    override fun prepareRenaming(element: PsiElement, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        super.prepareRenaming(element, newName, allRenames, scope)

        if (element !is KtParameter || newName == null) return

        val function = element.ownerFunction ?: return
        val originalDescriptor = function.resolveToDescriptor() as FunctionDescriptor
        val affectedCallables = getAffectedCallables(element.project, originalDescriptor.getDeepestSuperDeclarations())
        for (callable in affectedCallables) {
            if (!callable.canRefactor()) continue
            val parameter: PsiNamedElement? = when (callable) {
                is KtCallableDeclaration -> callable.valueParameters.firstOrNull { it.name == element.name }
                is PsiMethod -> callable.parameterList.parameters.firstOrNull { it.name == element.name }
                else -> null
            }
            if (parameter == null) continue
            allRenames[parameter] = newName.quoteIfNeeded()
        }
    }

    override fun renameElement(element: PsiElement, newName: String?, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
        super.renameElement(element, newName, usages, listener)

        usages.forEach { (it as? KtResolvableCollisionUsageInfo)?.apply() }
    }
}
