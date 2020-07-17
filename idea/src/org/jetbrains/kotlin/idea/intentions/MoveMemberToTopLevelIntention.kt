/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class MoveMemberToTopLevelIntention : MoveMemberOutOfObjectIntention(KotlinBundle.lazyMessage("move.to.top.level")) {
    override fun addConflicts(element: KtNamedDeclaration, conflicts: MultiMap<PsiElement, String>) {
        val packageViewDescriptor = element.findModuleDescriptor().getPackage(element.containingKtFile.packageFqName)
        val packageDescriptor = packageViewDescriptor.fragments.firstIsInstance<LazyPackageDescriptor>()
        val memberScope = packageDescriptor.getMemberScope()
        val packageName = packageViewDescriptor.fqName.asString().takeIf { it.isNotBlank() } ?: "default"

        val name = element.name ?: return

        val isRedeclaration = when (element) {
            is KtProperty -> memberScope.getVariableNames().any { name == it.identifier }
            is KtFunction -> {
                memberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_IDE).any {
                    descriptorsEqualWithSubstitution(element.descriptor, it, false)
                }
            }
            else -> false
        }

        if (isRedeclaration) {
            conflicts.putValue(
                element,
                KotlinBundle.message(
                    "package.0.already.contains.1",
                    packageName,
                    RefactoringUIUtil.getDescription(element, false)
                )
            )
        }
    }

    override fun getDestination(element: KtNamedDeclaration) = element.containingKtFile

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtNamedFunction && element !is KtProperty) return null
        if (element.containingClassOrObject !is KtObjectDeclaration) return null
        return element.nameIdentifier?.textRange
    }

}