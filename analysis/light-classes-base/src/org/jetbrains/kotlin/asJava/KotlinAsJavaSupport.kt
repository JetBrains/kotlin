/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript

abstract class KotlinAsJavaSupport {
    abstract fun getLightClass(classOrObject: KtClassOrObject): KtLightClass?

    abstract fun getLightClassForScript(script: KtScript): KtLightClass?

    abstract fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass

    abstract fun getLightFacade(file: KtFile): KtLightClassForFacade?

    abstract fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade

    abstract fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade>

    // Returns only immediately declared classes/objects, package classes are not included (they have no declarations)
    abstract fun findClassOrObjectDeclarationsInPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject>

    /*
    * Finds files whose package declaration is exactly {@code fqName}. For example, if a file declares
    *     package a.b.c
    * it will not be returned for fqName "a.b"
    *
    * If the resulting collection is empty, it means that this package has not other declarations than sub-packages
    */
    abstract fun findFilesForPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile>

    abstract fun findFilesForFacade(facadeFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile>

    abstract fun findFilesForFacadeByPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile>

    abstract fun findFilesForScript(scriptFqName: FqName, searchScope: GlobalSearchScope): Collection<KtScript>

    abstract fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject>

    abstract fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean

    abstract fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName>

    abstract fun getScriptClasses(scriptFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass>

    abstract fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade>

    abstract fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String>

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinAsJavaSupport {
            return project.getService( KotlinAsJavaSupport::class.java)
        }
    }
}