/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.asJava.FirLightClassForFacade
import org.jetbrains.kotlin.idea.asJava.classes.getOrCreateFirLightClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtScript

class IDEKotlinAsJavaFirSupport(project: Project) : IDEKotlinAsJavaSupport(project) {

    //TODO Make caching
    override fun createLightClassForFacade(manager: PsiManager, facadeClassFqName: FqName, searchScope: GlobalSearchScope): KtLightClass? {
        val sources = findFilesForFacade(facadeClassFqName, searchScope)
            .filterNot { it.isCompiled }

        if (sources.isEmpty()) return null

        return FirLightClassForFacade(manager, facadeClassFqName, sources)
    }

    override fun createLightClassForScript(script: KtScript): KtLightClass? = null

    override fun createLightClassForSourceDeclaration(classOrObject: KtClassOrObject): KtLightClass? =
        getOrCreateFirLightClass(classOrObject)
}