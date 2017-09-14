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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiImportStaticStatement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.highlighter.markers.actualsForExpected
import org.jetbrains.kotlin.idea.highlighter.markers.liftToExpected
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.ImportPath

abstract class RenameKotlinPsiProcessor : RenamePsiElementProcessor() {
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

    override fun prepareRenaming(element: PsiElement, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        if (newName == null) return

        val safeNewName = newName.quoteIfNeeded()

        if (!KotlinNameSuggester.isIdentifier(newName)) {
            allRenames[element] = safeNewName
        }

        val declaration = element.namedUnwrappedElement as? KtNamedDeclaration
        if (declaration != null) {
            declaration.liftToExpected()?.let { headerDeclaration ->
                allRenames[headerDeclaration] = safeNewName
                headerDeclaration.actualsForExpected().forEach { allRenames[it] = safeNewName }
            }
        }
    }

    protected var PsiElement.ambiguousImportUsages: List<UsageInfo>? by UserDataProperty(Key.create("AMBIGUOUS_IMPORT_USAGES"))

    protected fun UsageInfo.isAmbiguousImportUsage(): Boolean {
        val ref = reference as? PsiPolyVariantReference ?: return false
        val refElement = ref.element
        return refElement.parents.any { (it is KtImportDirective && !it.isAllUnder) || (it is PsiImportStaticStatement && !it.isOnDemand) }
               && ref.multiResolve(false).size > 1
    }

    override fun getPostRenameCallback(element: PsiElement, newName: String?, elementListener: RefactoringElementListener?): Runnable? {
        if (newName == null) return null

        return Runnable {
            element.ambiguousImportUsages?.forEach {
                val ref = it.reference as? PsiPolyVariantReference ?: return@forEach
                if (ref.multiResolve(false).isEmpty()) {
                    ref.handleElementRename(newName)
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
