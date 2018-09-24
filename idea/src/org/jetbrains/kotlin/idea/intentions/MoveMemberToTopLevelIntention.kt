/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class MoveMemberToTopLevelIntention : MoveMemberOutOfObjectIntention("Move to top level") {
    override fun addConflicts(element: KtNamedDeclaration, conflicts: MultiMap<PsiElement, String>) {
        val packageViewDescriptor = element.findModuleDescriptor().getPackage(element.containingKtFile.packageFqName)
        val packageDescriptor = packageViewDescriptor.fragments.firstIsInstance<LazyPackageDescriptor>()
        val memberScope = packageDescriptor.getMemberScope()
        val packageName = packageViewDescriptor.fqName.asString().takeIf { it.isNotBlank() } ?: "default"

        val name = element.name ?: return

        val isRedeclaration = when (element) {
            is KtProperty -> memberScope.getVariableNames().any { name == it.identifier }

            is KtFunction -> {
                memberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_IDE).filter {
                    descriptorsEqualWithSubstitution(element.descriptor, it, false)
                }.isNotEmpty()
            }

            else -> false
        }

        if (isRedeclaration) {
            conflicts.putValue(element, "Package '$packageName' already contains ${RefactoringUIUtil.getDescription(element, false)}")
        }
    }

    override fun getDestination(element: KtNamedDeclaration) = element.containingKtFile

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtNamedFunction && element !is KtProperty) return null
        if (element.containingClassOrObject !is KtObjectDeclaration) return null
        return element.nameIdentifier?.textRange
    }

}