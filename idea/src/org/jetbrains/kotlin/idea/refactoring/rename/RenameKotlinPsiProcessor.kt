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

import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.ArrayList
import kotlin.collections.*

abstract class RenameKotlinPsiProcessor : RenamePsiElementProcessor() {
    class MangledJavaRefUsageInfo(
        val manglingSuffix: String,
        element: PsiElement,
        ref: PsiReference,
        referenceElement: PsiElement
    ) : MoveRenameUsageInfo(
            referenceElement,
            ref,
            ref.getRangeInElement().getStartOffset(),
            ref.getRangeInElement().getEndOffset(),
            element,
            false
    )

    override fun canProcessElement(element: PsiElement): Boolean = element is KtNamedDeclaration

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        val searchParameters = KotlinReferencesSearchParameters(
            element,
            element.project.projectScope(),
            kotlinOptions = KotlinReferencesSearchOptions(searchForComponentConventions = false)
        )
        val references = ReferencesSearch.search(searchParameters).toMutableList()
        if (element is KtNamedFunction
            || (element is KtProperty && !element.isLocal)
            || (element is KtParameter && element.hasValOrVar())) {
            element.toLightMethods().flatMapTo(references) { MethodReferencesSearch.search(it) }
        }
        return references
    }

    override fun createUsageInfo(element: PsiElement, ref: PsiReference, referenceElement: PsiElement): UsageInfo {
        if (ref !is KtReference) {
            val targetElement = ref.resolve()
            if (targetElement is KtLightMethod && targetElement.isMangled) {
                KotlinTypeMapper.InternalNameMapper.getModuleNameSuffix(targetElement.name)?.let {
                    return MangledJavaRefUsageInfo(
                            it,
                            element,
                            ref,
                            referenceElement
                    )
                }
            }
        }
        return super.createUsageInfo(element, ref, referenceElement)
    }

    override fun getElementToSearchInStringsAndComments(element: PsiElement): PsiElement? {
        val unwrapped = element?.unwrapped ?: return null
        if ((unwrapped is KtDeclaration) && KtPsiUtil.isLocal(unwrapped as KtDeclaration)) return null
        return element
    }

    override fun getQualifiedNameAfterRename(element: PsiElement, newName: String, nonJava: Boolean): String? {
        if (!nonJava) return newName

        val qualifiedName = when (element) {
            is KtNamedDeclaration -> element.fqName?.asString() ?: element.name
            is PsiClass -> element.qualifiedName ?: element.name
            else -> return null
        }
        return PsiUtilCore.getQualifiedNameAfterRename(qualifiedName, newName)
    }

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val safeNewName = newName.quoteIfNeeded()

        if (!newName.isIdentifier()) {
            allRenames[element] = safeNewName
        }

        val declaration = element.namedUnwrappedElement as? KtNamedDeclaration
        if (declaration != null) {
            declaration.liftToExpected()?.let { expectDeclaration ->
                allRenames[expectDeclaration] = safeNewName
                expectDeclaration.actualsForExpected().forEach { allRenames[it] = safeNewName }
            }
        }
    }

    protected var PsiElement.ambiguousImportUsages: List<UsageInfo>? by UserDataProperty(Key.create("AMBIGUOUS_IMPORT_USAGES"))

    protected fun UsageInfo.isAmbiguousImportUsage(): Boolean {
        val ref = reference as? PsiPolyVariantReference ?: return false
        val refElement = ref.element
        return refElement.parents.any { (it is KtImportDirective && !it.isAllUnder) || (it is PsiImportStaticStatement && !it.isOnDemand) }
               && ref.multiResolve(false).mapNotNullTo(HashSet()) { it.element?.unwrapped }.size > 1
    }

    protected fun renameMangledUsageIfPossible(usage: UsageInfo, element: PsiElement, newName: String): Boolean {
        val chosenName = if (usage is MangledJavaRefUsageInfo) {
            KotlinTypeMapper.InternalNameMapper.mangleInternalName(newName, usage.manglingSuffix)
        } else {
            val reference = usage.reference
            if (reference is KtReference) {
                if (element is KtLightMethod && element.isMangled) {
                    KotlinTypeMapper.InternalNameMapper.demangleInternalName(newName)
                } else null
            } else null
        }
        if (chosenName == null) return false
        usage.reference?.handleElementRename(chosenName)
        return true
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<UsageInfo>,
        listener: RefactoringElementListener?
    ) {
        val simpleUsages = ArrayList<UsageInfo>(usages.size)
        for (usage in usages) {
            if (renameMangledUsageIfPossible(usage, element, newName)) continue
            simpleUsages += usage
        }

        RenameUtil.doRenameGenericNamedElement(element, newName, simpleUsages.toTypedArray(), listener)
    }

    override fun getPostRenameCallback(element: PsiElement, newName: String, elementListener: RefactoringElementListener): Runnable? {
        return Runnable {
            element.ambiguousImportUsages?.forEach {
                val ref = it.reference as? PsiPolyVariantReference ?: return@forEach
                if (ref.multiResolve(false).isEmpty()) {
                    if (!renameMangledUsageIfPossible(it, element, newName)) {
                        ref.handleElementRename(newName)
                    }
                }
                else {
                    ref.element?.getStrictParentOfType<KtImportDirective>()?.let { importDirective ->
                        val fqName = importDirective.importedFqName!!
                        val newFqName = fqName.parent().child(Name.identifier(newName))
                        val importList = importDirective.parent as KtImportList
                        if (importList.imports.none { it.importedFqName == newFqName }) {
                            val newImportDirective = KtPsiFactory(element).createImportDirective(ImportPath(newFqName, false))
                            importDirective.parent.addAfter(newImportDirective, importDirective)
                        }
                    }
                }
            }
            element.ambiguousImportUsages = null
        }
    }
}
