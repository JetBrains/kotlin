/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.idea.refactoring.withExpectedActuals
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.SmartList
import java.util.*

// FIX ME WHEN BUNCH 191 REMOVED
abstract class RenameKotlinClassifierProcessorCompat : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return element is KtClassOrObject || element is KtLightClass || element is KtConstructor<*> || element is KtTypeAlias
    }

    override fun isToSearchInComments(psiElement: PsiElement) = KotlinRefactoringSettings.instance.RENAME_SEARCH_IN_COMMENTS_FOR_CLASS

    override fun setToSearchInComments(element: PsiElement, enabled: Boolean) {
        KotlinRefactoringSettings.instance.RENAME_SEARCH_IN_COMMENTS_FOR_CLASS = enabled
    }

    override fun isToSearchForTextOccurrences(element: PsiElement) = KotlinRefactoringSettings.instance.RENAME_SEARCH_FOR_TEXT_FOR_CLASS

    override fun setToSearchForTextOccurrences(element: PsiElement, enabled: Boolean) {
        KotlinRefactoringSettings.instance.RENAME_SEARCH_FOR_TEXT_FOR_CLASS = enabled
    }

    override fun substituteElementToRename(element: PsiElement, editor: Editor?) = getClassOrObject(element)

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        super.prepareRenaming(element, newName, allRenames)

        val classOrObject = getClassOrObject(element) as? KtClassOrObject ?: return

        classOrObject.withExpectedActuals().forEach {
            val file = it.containingKtFile
            val virtualFile = file.virtualFile
            if (virtualFile != null) {
                val nameWithoutExtensions = virtualFile.nameWithoutExtension
                if (nameWithoutExtensions == it.name) {
                    val newFileName = newName + "." + virtualFile.extension
                    allRenames.put(file, newFileName)
                    forElement(file).prepareRenaming(file, newFileName, allRenames)
                }
            }
        }
    }

    protected fun processFoundReferences(
        element: PsiElement,
        references: Collection<PsiReference>
    ): Collection<PsiReference> {
        if (element is KtObjectDeclaration && element.isCompanion()) {
            return references.filter { !it.isCompanionObjectClassReference() }
        }
        return references
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
        newName: String,
        allRenames: MutableMap<out PsiElement, String>,
        result: MutableList<UsageInfo>
    ) {
        val declaration = element.namedUnwrappedElement as? KtNamedDeclaration ?: return

        val collisions = SmartList<UsageInfo>()
        checkRedeclarations(declaration, newName, collisions)
        checkOriginalUsagesRetargeting(declaration, newName, result, collisions)
        checkNewNameUsagesRetargeting(declaration, newName, collisions)
        result += collisions
    }

    private fun getClassOrObject(element: PsiElement?): PsiElement? = when (element) {
        is KtLightClass ->
            when (element) {
                is KtLightClassForSourceDeclaration -> element.kotlinOrigin
                is KtLightClassForFacade -> element
                else -> throw AssertionError("Should not be suggested to rename element of type " + element::class.java + " " + element)
            }

        is KtConstructor<*> ->
            element.getContainingClassOrObject()

        is KtClassOrObject, is KtTypeAlias -> element

        else -> null
    }

    override fun renameElement(element: PsiElement, newName: String, usages: Array<UsageInfo>, listener: RefactoringElementListener?) {
        val simpleUsages = ArrayList<UsageInfo>(usages.size)
        val ambiguousImportUsages = com.intellij.util.SmartList<UsageInfo>()
        for (usage in usages) {
            if (usage.isAmbiguousImportUsage()) {
                ambiguousImportUsages += usage
            } else {
                simpleUsages += usage
            }
        }
        element.ambiguousImportUsages = ambiguousImportUsages

        super.renameElement(element, newName, simpleUsages.toTypedArray(), listener)

        usages.forEach { (it as? KtResolvableCollisionUsageInfo)?.apply() }
    }
}
