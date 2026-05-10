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
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript

abstract class KotlinAsJavaSupport {
    /**
     * Creates a [KtLightClass] for the given [classOrObject].
     *
     * [GlobalSearchScope] represents the scope which should be used to find a suitable context module for the produced [KtLightClass].
     * If `null`, the declaration-site module is used.
     */
    abstract fun getLightClass(classOrObject: KtClassOrObject, searchScope: GlobalSearchScope? = null): KtLightClass?

    /**
     * Creates a [KtLightClass] for the given [script].
     *
     * [GlobalSearchScope] represents the scope which should be used to find a suitable context module for the produced [KtLightClass].
     * If `null`, the declaration-site module is used.
     */
    abstract fun getLightClassForScript(script: KtScript, searchScope: GlobalSearchScope? = null): KtLightClass?

    abstract fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass

    /**
     * Creates a [KtLightClass] for the given [file].
     *
     * [GlobalSearchScope] represents the scope which should be used to find a suitable context module
     * for the produced [KtLightClassForFacade]. If `null`, the declaration-site module is used.
     */
    abstract fun getLightFacade(file: KtFile, searchScope: GlobalSearchScope? = null): KtLightClassForFacade?

    abstract fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade

    /**
     * Returns all facade classes for the given [facadeFqName] found in [scope].
     *
     * All [KtLightClassForFacade]s are created in the context of a JVM module:
     * - If the original file was located in a JVM module, this module is used as a context.
     * - If the original file was located in a COMMON module, a suitable JVM implementation module is passed as a context.
     *   This JVM module is the first implementation module found that is covered by the provided [scope],
     *   see [GlobalSearchScope.isSearchInModuleContent].
     */
    abstract fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade>

    /**
     * Returns classes and objects declared in [packageFqName] that fall under [searchScope].
     */
    abstract fun findClassOrObjectDeclarationsInPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject>

    /**
     * Finds [KtFile]s whose package declaration is exactly [packageFqName].
     *
     * For example, if a file declares
     * ```kotlin
     * package a.b.c
     * ```
     * it will not be returned for `a.b` [packageFqName].
     *
     * If the package has no declarations other than sub-packages, returns an empty collection.
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

    /**
     * Provides a resolution scope for [FakeFileForLightClass.getFileResolveScope].
     * This scope is then passed to [KotlinAsJavaSupport] from [JavaElementFinder] when resolving declarations in the [file].
     */
    abstract fun getResolutionScope(file: FakeFileForLightClass): GlobalSearchScope

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinAsJavaSupport {
            return project.getService(KotlinAsJavaSupport::class.java)
        }
    }
}