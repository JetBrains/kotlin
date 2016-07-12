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
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.utils.SmartList

abstract class RenameKotlinPsiProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element is KtNamedDeclaration

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        val references = SmartList<PsiReference>(super.findReferences(element))
        if (element is KtNamedFunction
            || (element is KtProperty && !element.isLocal)
            || (element is KtParameter && element.hasValOrVar())) {
            element.toLightMethods().flatMapTo(references) { MethodReferencesSearch.search(it) }
        }
        return references
    }

    override fun prepareRenaming(element: PsiElement, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        if (newName != null && !KotlinNameSuggester.isIdentifier(newName)) {
            allRenames[element] = newName.quoteIfNeeded()
        }
    }
}
