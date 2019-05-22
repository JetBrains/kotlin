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
import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenameJavaMethodProcessor
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.InternalNameMapper.demangleInternalName
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.InternalNameMapper.getModuleNameSuffix
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.InternalNameMapper.mangleInternalName
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.Pass
import org.jetbrains.kotlin.idea.refactoring.checkSuperMethods
import org.jetbrains.kotlin.idea.refactoring.checkSuperMethodsWithPopup
import org.jetbrains.kotlin.idea.refactoring.dropOverrideKeywordIfNecessary
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.search.declarationsSearch.findDeepestSuperMethodsKotlinAware
import org.jetbrains.kotlin.idea.search.declarationsSearch.findDeepestSuperMethodsNoWrapping
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

class RenameKotlinFunctionProcessor : RenameKotlinPsiProcessor() {
    private val javaMethodProcessorInstance = RenameJavaMethodProcessor()

    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtNamedFunction || (element is KtLightMethod && element.kotlinOrigin is KtNamedFunction) || element is FunctionWithSupersWrapper
    }

    override fun isToSearchInComments(psiElement: PsiElement) = KotlinRefactoringSettings.instance.RENAME_SEARCH_IN_COMMENTS_FOR_METHOD

    override fun setToSearchInComments(element: PsiElement, enabled: Boolean) {
        KotlinRefactoringSettings.instance.RENAME_SEARCH_IN_COMMENTS_FOR_METHOD = enabled
    }

    override fun isToSearchForTextOccurrences(element: PsiElement) = KotlinRefactoringSettings.instance.RENAME_SEARCH_FOR_TEXT_FOR_METHOD

    override fun setToSearchForTextOccurrences(element: PsiElement, enabled: Boolean) {
        KotlinRefactoringSettings.instance.RENAME_SEARCH_FOR_TEXT_FOR_METHOD = enabled
    }

    private fun getJvmName(element: PsiElement): String? {
        val descriptor = (element.unwrapped as? KtFunction)?.unsafeResolveToDescriptor() as? FunctionDescriptor ?: return null
        return DescriptorUtils.getJvmName(descriptor)
    }

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        val allReferences = super.findReferences(element)
        return when {
            getJvmName(element) == null -> allReferences
            element is KtElement -> allReferences.filterIsInstance<KtReference>()
            element is KtLightElement<*, *> -> allReferences.filterNot { it is KtReference }
            else -> emptyList()
        }
    }

    override fun findCollisions(
        element: PsiElement,
        newName: String,
        allRenames: Map<out PsiElement, String>,
        result: MutableList<UsageInfo>
    ) {
        val declaration = element.unwrapped as? KtNamedFunction ?: return
        checkConflictsAndReplaceUsageInfos(element, allRenames, result)
        result += SmartList<UsageInfo>().also { collisions ->
            checkRedeclarations(declaration, newName, collisions)
            checkOriginalUsagesRetargeting(declaration, newName, result, collisions)
            checkNewNameUsagesRetargeting(declaration, newName, collisions)
        }
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

    private fun substituteForExpectOrActual(element: PsiElement?) = (element?.namedUnwrappedElement as? KtNamedDeclaration)?.liftToExpected()

    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement?  {
        substituteForExpectOrActual(element)?.let { return it }

        val wrappedMethod = wrapPsiMethod(element) ?: return element

        val deepestSuperMethods = findDeepestSuperMethodsKotlinAware(wrappedMethod)
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
            } else {
                substitutedJavaElement
            }
            renameCallback.pass(elementToProcess)
        }

        substituteForExpectOrActual(element)?.let { return preprocessAndPass(it) }

        val wrappedMethod = wrapPsiMethod(element)
        val deepestSuperMethods = if (wrappedMethod != null) {
            findDeepestSuperMethodsKotlinAware(wrappedMethod)
        } else {
            findDeepestSuperMethodsNoWrapping(element)
        }
        when {
            deepestSuperMethods.isEmpty() -> preprocessAndPass(element)
            wrappedMethod != null && (wrappedMethod.isConstructor || element !is KtNamedFunction) -> {
                javaMethodProcessorInstance.substituteElementToRename(wrappedMethod, editor, Pass(::preprocessAndPass))
            }
            else -> {
                val declaration = element.unwrapped as? KtNamedFunction ?: return
                checkSuperMethodsWithPopup(declaration, deepestSuperMethods.toList(), "Rename", editor) {
                    preprocessAndPass(if (it.size > 1) FunctionWithSupersWrapper(declaration, it) else wrappedMethod ?: element)
                }
            }
        }
    }

    override fun createRenameDialog(
        project: Project,
        element: PsiElement,
        nameSuggestionContext: PsiElement?,
        editor: Editor?
    ): RenameDialog {
        val elementForDialog = (element as? FunctionWithSupersWrapper)?.originalDeclaration ?: element
        return object : RenameDialog(project, elementForDialog, nameSuggestionContext, editor) {
            override fun createRenameProcessor(newName: String) =
                RenameProcessor(getProject(), element, newName, isSearchInComments, isSearchInNonJavaFiles)
        }
    }

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        super.prepareRenaming(element, newName, allRenames, scope)

        if (element is KtLightMethod && getJvmName(element) == null) {
            (element.kotlinOrigin as? KtNamedFunction)?.let { allRenames[it] = newName }
        }
        if (element is FunctionWithSupersWrapper) {
            allRenames.remove(element)
        }
        val originalName = (element.unwrapped as? KtNamedFunction)?.name ?: return
        for (declaration in ((element as? FunctionWithSupersWrapper)?.supers ?: listOf(element))) {
            val psiMethod = wrapPsiMethod(declaration) ?: continue
            allRenames[declaration] = newName
            val baseName = psiMethod.name
            val newBaseName = if (demangleInternalName(baseName) == originalName) {
                mangleInternalName(newName, getModuleNameSuffix(baseName)!!)
            } else newName
            if (psiMethod.containingClass != null) {
                psiMethod.forEachOverridingMethod(scope) {
                    val overrider = (it as? PsiMirrorElement)?.prototype as? PsiMethod ?: it

                    if (overrider is SyntheticElement) return@forEachOverridingMethod true

                    val overriderName = overrider.name
                    val newOverriderName = RefactoringUtil.suggestNewOverriderName(overriderName, baseName, newBaseName)
                    if (newOverriderName != null) {
                        RenameProcessor.assertNonCompileElement(overrider)
                        allRenames[overrider] = newOverriderName
                    }
                    return@forEachOverridingMethod true
                }
            }
        }
    }

    override fun renameElement(element: PsiElement, newName: String, usages: Array<UsageInfo>, listener: RefactoringElementListener?) {
        val simpleUsages = ArrayList<UsageInfo>(usages.size)
        val ambiguousImportUsages = SmartList<UsageInfo>()
        for (usage in usages) {
            if (usage is LostDefaultValuesInOverridingFunctionUsageInfo) {
                usage.apply()
                continue
            }

            if (usage.isAmbiguousImportUsage()) {
                ambiguousImportUsages += usage
            } else {
                if (!renameMangledUsageIfPossible(usage, element, newName)) {
                    simpleUsages += usage
                }
            }
        }
        element.ambiguousImportUsages = ambiguousImportUsages

        RenameUtil.doRenameGenericNamedElement(element, newName, simpleUsages.toTypedArray(), listener)

        usages.forEach { (it as? KtResolvableCollisionUsageInfo)?.apply() }

        (element.unwrapped as? KtNamedDeclaration)?.let(::dropOverrideKeywordIfNecessary)
    }

    private fun wrapPsiMethod(element: PsiElement?): PsiMethod? = when (element) {
        is PsiMethod -> element
        is KtNamedFunction, is KtSecondaryConstructor -> runReadAction {
            LightClassUtil.getLightClassMethod(element as KtFunction)
        }
        else -> throw IllegalStateException("Can't be for element $element there because of canProcessElement()")
    }
}
