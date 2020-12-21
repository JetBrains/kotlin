/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.compiler

import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch.SearchParameters
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.idea.caches.resolve.util.javaResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.util.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.SealedClassInheritorsProvider

object IdeSealedClassInheritorsProvider : SealedClassInheritorsProvider() {

    override fun computeSealedSubclasses(
        sealedClass: ClassDescriptor,
        allowSealedInheritorsInDifferentFilesOfSamePackage: Boolean
    ): Collection<ClassDescriptor> {

        val sealedKtClass = sealedClass.findPsi() as? KtClass ?: return emptyList()
        val searchScope: SearchScope = if (allowSealedInheritorsInDifferentFilesOfSamePackage) {
            val module = sealedKtClass.module ?: return emptyList()
            val moduleSourceScope = GlobalSearchScope.moduleScope(module)
            val containingPackage = sealedClass.containingPackage() ?: return emptyList()
            val psiPackage = JavaDirectoryService.getInstance().getPackage(sealedKtClass.containingFile.containingDirectory)
                ?: JavaPsiFacade.getInstance(sealedKtClass.project).findPackage(containingPackage.asString())
                ?: return emptyList()
            val packageScope = PackageScope(psiPackage, false, false)
            moduleSourceScope.intersectWith(packageScope)
        } else {
            GlobalSearchScope.fileScope(sealedKtClass.containingFile) // Kotlin version prior to 1.5
        }

        val lightClass = sealedKtClass.toLightClass() ?: return emptyList()
        val searchParameters = SearchParameters(lightClass, searchScope, false, true, false)

        return ClassInheritorsSearch.search(searchParameters)
            .map mapper@{
                val resolutionFacade = it.javaResolutionFacade() ?: return@mapper null
                it.resolveToDescriptor(resolutionFacade)
            }.filterNotNull()
            .sortedBy(ClassDescriptor::getName) // order needs to be stable (at least for tests)
    }
}