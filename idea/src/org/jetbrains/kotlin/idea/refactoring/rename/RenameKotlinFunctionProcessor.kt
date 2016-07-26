/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameJavaMethodProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.dropOverrideKeywordIfNecessary
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

class RenameKotlinFunctionProcessor : RenameKotlinPsiProcessor() {
    private val javaMethodProcessorInstance = RenameJavaMethodProcessor()

    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtNamedFunction || (element is KtLightMethod && element.kotlinOrigin is KtNamedFunction)
    }

    override fun isToSearchInComments(psiElement: PsiElement) = JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_METHOD

    override fun setToSearchInComments(element: PsiElement, enabled: Boolean) {
        JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_METHOD = enabled
    }

    override fun isToSearchForTextOccurrences(element: PsiElement) = JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_METHOD

    override fun setToSearchForTextOccurrences(element: PsiElement, enabled: Boolean) {
        JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_METHOD = enabled
    }

    private fun getJvmName(element: PsiElement): String? {
        val descriptor = (element.unwrapped as? KtFunction)?.resolveToDescriptor() as? FunctionDescriptor ?: return null
        return DescriptorUtils.getJvmName(descriptor)
    }

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        val allReferences = super.findReferences(element)
        return when {
            getJvmName(element) == null -> allReferences
            element is KtElement -> allReferences.filter { it is KtReference }
            element is KtLightElement<*, *> -> allReferences.filterNot { it is KtReference }
            else -> emptyList()
        }
    }

    override fun findCollisions(
            element: PsiElement,
            newName: String?,
            allRenames: Map<out PsiElement, String>,
            result: MutableList<UsageInfo>
    ) {
        checkConflictsAndReplaceUsageInfos(element, allRenames, result)
    }

    override fun substituteElementToRename(element: PsiElement?, editor: Editor?): PsiElement?  {
        val wrappedMethod = wrapPsiMethod(element) ?: return element

        // Use java dialog to ask we should rename function with the base element
        val substitutedJavaElement = javaMethodProcessorInstance.substituteElementToRename(wrappedMethod, editor)

        if (substitutedJavaElement is KtLightMethod && element is KtDeclaration) {
            return substitutedJavaElement.kotlinOrigin as? KtNamedFunction
        }

        return substitutedJavaElement
    }

    override fun prepareRenaming(element: PsiElement, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        super.prepareRenaming(element, newName, allRenames, scope)
        if (element is KtLightMethod && newName != null && getJvmName(element) == null) {
            (element.kotlinOrigin as? KtNamedFunction)?.let { allRenames[it] = newName }
        }
        val psiMethod = wrapPsiMethod(element)
        if (psiMethod?.containingClass != null) {
            javaMethodProcessorInstance.prepareRenaming(psiMethod, newName, allRenames, scope)
        }
    }

    override fun renameElement(element: PsiElement, newName: String?, usages: Array<UsageInfo>, listener: RefactoringElementListener?) {
        val simpleUsages = ArrayList<UsageInfo>(usages.size)
        val ambiguousImportUsages = SmartList<UsageInfo>()
        for (usage in usages) {
            if (usage is LostDefaultValuesInOverridingFunctionUsageInfo) {
                usage.apply()
                continue
            }

            if (usage.isAmbiguousImportUsage()) {
                ambiguousImportUsages += usage
            }
            else {
                simpleUsages += usage
            }
        }
        element.ambiguousImportUsages = ambiguousImportUsages

        RenameUtil.doRenameGenericNamedElement(element, newName, simpleUsages.toTypedArray(), listener)

        (element.unwrapped as? KtNamedDeclaration)?.let { dropOverrideKeywordIfNecessary(it) }
    }

    private fun wrapPsiMethod(element: PsiElement?): PsiMethod? = when (element) {
        is KtLightMethod -> element
        is KtNamedFunction, is KtSecondaryConstructor -> runReadAction {
            LightClassUtil.getLightClassMethod(element as KtFunction)
        }
        else -> throw IllegalStateException("Can't be for element $element there because of canProcessElement()")
    }
}
