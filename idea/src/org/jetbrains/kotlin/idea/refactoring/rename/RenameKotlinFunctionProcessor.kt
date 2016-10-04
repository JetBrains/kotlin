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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenameJavaMethodProcessor
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.Pass
import org.jetbrains.kotlin.idea.refactoring.checkSuperMethods
import org.jetbrains.kotlin.idea.refactoring.checkSuperMethodsWithPopup
import org.jetbrains.kotlin.idea.refactoring.dropOverrideKeywordIfNecessary
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.lang.IllegalStateException
import java.util.*

class RenameKotlinFunctionProcessor : RenameKotlinPsiProcessor() {
    private val javaMethodProcessorInstance = RenameJavaMethodProcessor()

    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtNamedFunction || (element is KtLightMethod && element.kotlinOrigin is KtNamedFunction) || element is FunctionWithSupersWrapper
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
        if (newName == null) return
        val declaration = element.unwrapped as? KtNamedFunction ?: return
        val descriptor = declaration.resolveToDescriptor()
        checkConflictsAndReplaceUsageInfos(element, allRenames, result)
        checkRedeclarations(descriptor, newName, result)
    }

    class FunctionWithSupersWrapper(
            val originalDeclaration: KtNamedFunction,
            val supers: List<PsiElement>
    ) : KtLightElement<KtNamedFunction, KtNamedFunction>, PsiNamedElement by originalDeclaration {
        override val kotlinOrigin: KtNamedFunction?
            get() = originalDeclaration
        override val clsDelegate: KtNamedFunction
            get() = originalDeclaration
    }

    override fun substituteElementToRename(element: PsiElement?, editor: Editor?): PsiElement?  {
        val wrappedMethod = wrapPsiMethod(element) ?: return element

        val deepestSuperMethods = wrappedMethod.findDeepestSuperMethods()
        val substitutedJavaElement = when {
            deepestSuperMethods.isEmpty() -> return element
            wrappedMethod.isConstructor || deepestSuperMethods.size == 1 || element !is KtNamedFunction -> {
                javaMethodProcessorInstance.substituteElementToRename(wrappedMethod, editor)
            }
            else -> {
                val chosenElements = checkSuperMethods(element, null, "rename")
                if (chosenElements.size > 1) FunctionWithSupersWrapper(element, chosenElements) else wrappedMethod
            }
        }

        if (substitutedJavaElement is KtLightMethod && element is KtDeclaration) {
            return substitutedJavaElement.kotlinOrigin as? KtNamedFunction
        }

        return substitutedJavaElement
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor, renameCallback: Pass<PsiElement>) {
        fun preprocessAndPass(substitutedJavaElement: PsiElement) {
            val elementToProcess = if (substitutedJavaElement is KtLightMethod && element is KtDeclaration) {
                substitutedJavaElement.kotlinOrigin as? KtNamedFunction
            }
            else {
                substitutedJavaElement
            }
            renameCallback.pass(elementToProcess)
        }

        val wrappedMethod = wrapPsiMethod(element) ?: return

        val deepestSuperMethods = wrappedMethod.findDeepestSuperMethods()
        when {
            deepestSuperMethods.isEmpty() -> return
            wrappedMethod.isConstructor || element !is KtNamedFunction -> {
                javaMethodProcessorInstance.substituteElementToRename(wrappedMethod, editor, Pass(::preprocessAndPass))
            }
            else -> {
                val declaration = element.unwrapped as? KtNamedDeclaration ?: return
                checkSuperMethodsWithPopup(declaration, deepestSuperMethods.toList(), "Rename", editor) {
                    preprocessAndPass(if (it.size > 1) FunctionWithSupersWrapper(element, it) else wrappedMethod)
                }
            }
        }
    }

    override fun createRenameDialog(project: Project, element: PsiElement, nameSuggestionContext: PsiElement?, editor: Editor?): RenameDialog {
        val elementForDialog = (element as? FunctionWithSupersWrapper)?.originalDeclaration ?: element
        return object : RenameDialog(project, elementForDialog, nameSuggestionContext, editor) {
            override fun createRenameProcessor(newName: String) = RenameProcessor(getProject(), element, newName, isSearchInComments, isSearchInNonJavaFiles)
        }
    }

    override fun prepareRenaming(element: PsiElement, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        super.prepareRenaming(element, newName, allRenames, scope)

        if (newName == null) return

        if (element is KtLightMethod && getJvmName(element) == null) {
            (element.kotlinOrigin as? KtNamedFunction)?.let { allRenames[it] = newName }
        }
        if (element is FunctionWithSupersWrapper) {
            allRenames.remove(element)
        }
        for (declaration in (if (element is FunctionWithSupersWrapper) element.supers else listOf(element))) {
            val psiMethod = wrapPsiMethod(declaration) ?: continue
            allRenames[declaration] = newName
            if (psiMethod.containingClass != null) {
                javaMethodProcessorInstance.prepareRenaming(psiMethod, newName, allRenames, scope)
            }
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
        is PsiMethod -> element
        is KtNamedFunction, is KtSecondaryConstructor -> runReadAction {
            LightClassUtil.getLightClassMethod(element as KtFunction)
        }
        else -> throw IllegalStateException("Can't be for element $element there because of canProcessElement()")
    }
}
