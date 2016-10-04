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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.SmartList
import java.util.*

class RenameKotlinClassProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtClassOrObject || element is KtLightClass || element is KtConstructor<*>
    }

    override fun isToSearchInComments(psiElement: PsiElement) = JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_CLASS

    override fun setToSearchInComments(element: PsiElement, enabled: Boolean) {
        JavaRefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_CLASS = enabled
    }

    override fun isToSearchForTextOccurrences(element: PsiElement) = JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_CLASS

    override fun setToSearchForTextOccurrences(element: PsiElement, enabled: Boolean) {
        JavaRefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_CLASS = enabled
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?) = getClassOrObject(element)

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        super.prepareRenaming(element, newName, allRenames)

        val classOrObject = getClassOrObject(element) as? KtClassOrObject ?: return

        val file = classOrObject.getContainingKtFile()

        val virtualFile = file.virtualFile
        if (virtualFile != null) {
            val nameWithoutExtensions = virtualFile.nameWithoutExtension
            if (nameWithoutExtensions == classOrObject.name) {
                val newFileName = newName + "." + virtualFile.extension
                allRenames.put(file, newFileName)
                RenamePsiElementProcessor.forElement(file).prepareRenaming(file, newFileName, allRenames)
            }
        }
    }

    override fun getQualifiedNameAfterRename(element: PsiElement, newName: String?, nonJava: Boolean): String? {
        if (!nonJava) return newName

        val qualifiedName = when (element) {
            is KtClassOrObject -> element.fqName?.asString() ?: element.name
            is PsiClass -> element.qualifiedName ?: element.name
            else -> return null
        }
        return PsiUtilCore.getQualifiedNameAfterRename(qualifiedName, newName)
    }

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        if (element is KtObjectDeclaration && element.isCompanion()) {
            return super.findReferences(element).filter { !it.isCompanionObjectClassReference() }
        }
        return super.findReferences(element)
    }

    private fun PsiReference.isCompanionObjectClassReference(): Boolean {
        if (this !is KtSimpleNameReference) {
            return false
        }
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        return bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element] != null
    }

    override fun findCollisions(
            element: PsiElement,
            newName: String?,
            allRenames: MutableMap<out PsiElement, String>,
            result: MutableList<UsageInfo>
    ) {
        if (newName == null) return
        val declaration = element.namedUnwrappedElement as? KtNamedDeclaration ?: return
        val descriptor = declaration.resolveToDescriptor() as ClassDescriptor

        val collisions = SmartList<UsageInfo>()
        checkRedeclarations(descriptor, newName, collisions)
        checkOriginalUsagesRetargeting(declaration, newName, result, collisions)
        checkNewNameUsagesRetargeting(declaration, newName, collisions)
        result += collisions
    }

    private fun getClassOrObject(element: PsiElement?): PsiElement? = when (element) {
        is KtLightClass ->
            when (element) {
                is KtLightClassForSourceDeclaration -> element.kotlinOrigin
                is KtLightClassForFacade -> element
                else -> throw AssertionError("Should not be suggested to rename element of type " + element.javaClass + " " + element)
            }

        is KtConstructor<*> ->
            element.getContainingClassOrObject()

        else ->
            element as? KtClassOrObject
    }

    override fun renameElement(element: PsiElement, newName: String?, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
        val simpleUsages = ArrayList<UsageInfo>(usages.size)
        val ambiguousImportUsages = com.intellij.util.SmartList<UsageInfo>()
        for (usage in usages) {
            if (usage.isAmbiguousImportUsage()) {
                ambiguousImportUsages += usage
            }
            else {
                simpleUsages += usage
            }
        }
        element.ambiguousImportUsages = ambiguousImportUsages

        super.renameElement(element, newName, simpleUsages.toTypedArray(), listener)

        usages.forEach { (it as? KtResolvableCollisionUsageInfo)?.apply() }
    }
}
