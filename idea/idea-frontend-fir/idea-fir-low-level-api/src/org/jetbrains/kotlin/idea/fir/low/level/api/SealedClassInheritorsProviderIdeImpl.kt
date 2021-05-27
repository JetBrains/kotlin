/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.module.Module
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PackageScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.idea.util.classIdIfNonLocal
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import java.util.concurrent.ConcurrentHashMap

class SealedClassInheritorsProviderIdeImpl : SealedClassInheritorsProvider() {
    val cache = ConcurrentHashMap<ClassId, List<ClassId>>()

    @OptIn(SealedClassInheritorsProviderInternals::class)
    override fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId> {
        require(firClass.isSealed)
        firClass.sealedInheritorsAttr?.let { return it }
        return cache.computeIfAbsent(firClass.classId) { getInheritors(firClass) }
    }

    private fun getInheritors(firClass: FirRegularClass): List<ClassId> {
        // TODO fix for non-source classes
        val sealedKtClass = firClass.psi as? KtClass ?: return emptyList()
        val module = sealedKtClass.module ?: return emptyList()
        val containingPackage = firClass.classId.packageFqName

        val psiPackage = KotlinJavaPsiFacade.getInstance(sealedKtClass.project)
            .findPackage(containingPackage.asString(), GlobalSearchScope.moduleScope(module))
            ?: getPackageViaDirectoryService(sealedKtClass)
            ?: return emptyList()

        val kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(sealedKtClass.project)
        val lightClass = sealedKtClass.toLightClass() ?: kotlinAsJavaSupport.getFakeLightClass(sealedKtClass)

        val searchScope: SearchScope = getSearchScope(module, psiPackage)
        val searchParameters = ClassInheritorsSearch.SearchParameters(lightClass, searchScope, false, true, false)
        val subclasses = ClassInheritorsSearch.search(searchParameters)
            .mapNotNull { it.classIdIfNonLocal() }
            .toMutableList()

        // Enforce a deterministic order on the result.
        subclasses.sortBy { it.toString() }
        return subclasses
    }

    private fun getSearchScope(module: Module, psiPackage: PsiPackage): GlobalSearchScope {
        val packageScope = PackageScope(psiPackage, false, false)
        // MPP multiple common modules are not supported!!
        return module.moduleScope.intersectWith(packageScope)
    }

    private fun getPackageViaDirectoryService(ktClass: KtClass): PsiPackage? {
        val directory = ktClass.containingFile.containingDirectory ?: return null
        return JavaDirectoryService.getInstance().getPackage(directory)
    }
}