/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript

abstract class KotlinAsJavaSupport {
    // Returns only immediately declared classes/objects, package classes are not included (they have no declarations)
    abstract fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject>

    /*
    * Finds files whose package declaration is exactly {@code fqName}. For example, if a file declares
    *     package a.b.c
    * it will not be returned for fqName "a.b"
    *
    * If the resulting collection is empty, it means that this package has not other declarations than sub-packages
    */
    abstract fun findFilesForPackage(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile>

    abstract fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject>

    abstract fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean

    abstract fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName>

    abstract fun getLightClass(classOrObject: KtClassOrObject): KtLightClass?

    abstract fun getLightClassForScript(script: KtScript): KtLightClass?

    abstract fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getScriptClasses(scriptFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String>

    abstract fun findFilesForFacade(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtFile>

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinAsJavaSupport {
            return ServiceManager.getService(
                project,
                KotlinAsJavaSupport::class.java
            )
        }
    }
}