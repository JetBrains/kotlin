/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.references.getImportAlias
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinMethodReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.liftToExpected
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.ArrayList
import kotlin.collections.*

// FIX ME WHEN BUNCH 191 REMOVED
abstract class RenameKotlinPsiProcessorCompat : RenamePsiElementProcessor() {
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

    protected fun findReferences(
        element: PsiElement,
        searchParameters: KotlinReferencesSearchParameters
    ): Collection<PsiReference> {
        val references = ReferencesSearch.search(searchParameters).toMutableSet()
        if (element is KtNamedFunction || (element is KtProperty && !element.isLocal) || (element is KtParameter && element.hasValOrVar())) {
            element.toLightMethods().flatMapTo(references) { method ->
                MethodReferencesSearch.search(
                    KotlinMethodReferencesSearchParameters(
                        method,
                        kotlinOptions = KotlinReferencesSearchOptions(
                            acceptImportAlias = false
                        )
                    )
                )
            }
        }
        return references.filter {
            // have to filter so far as
            // - text-matched reference could be named as imported alias and found in ReferencesSearch
            // - MethodUsagesSearcher could create its own MethodReferencesSearchParameters regardless provided one
            it.element.getNonStrictParentOfType<KtImportDirective>() != null || it.getImportAlias() == null
        }
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
        val unwrapped = element.unwrapped ?: return null
        if ((unwrapped is KtDeclaration) && KtPsiUtil.isLocal(unwrapped)) return null
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
        return refElement.parents
            .any { (it is KtImportDirective && !it.isAllUnder) || (it is PsiImportStaticStatement && !it.isOnDemand) } && ref.multiResolve(
            false
        ).mapNotNullTo(HashSet()) { it.element?.unwrapped }.size > 1
    }

    protected fun renameMangledUsageIfPossible(usage: UsageInfo, element: PsiElement, newName: String): Boolean {
        val chosenName = (if (usage is MangledJavaRefUsageInfo) {
            KotlinTypeMapper.InternalNameMapper.mangleInternalName(newName, usage.manglingSuffix)
        } else {
            val reference = usage.reference
            if (reference is KtReference) {
                if (element is KtLightMethod && element.isMangled) {
                    KotlinTypeMapper.InternalNameMapper.demangleInternalName(newName)
                } else null
            } else null
        }) ?: return false
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
        ForeignUsagesRenameProcessor.processAll(element, newName, usages, fallbackHandler = { usage ->
            if (renameMangledUsageIfPossible(usage, element, newName)) return@processAll
            simpleUsages += usage
        })

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
                } else {
                    ref.element.getStrictParentOfType<KtImportDirective>()?.let { importDirective ->
                        val fqName = importDirective.importedFqName!!
                        val newFqName = fqName.parent().child(Name.identifier(newName))
                        val importList = importDirective.parent as KtImportList
                        if (importList.imports.none { directive -> directive.importedFqName == newFqName }) {
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
