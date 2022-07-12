/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript

class KotlinK1LightClassFactory : KotlinLightClassFactory {
    override fun createClass(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration? =
        CachedValuesManager.getCachedValue(classOrObject) {
            CachedValueProvider.Result.create(
                createClassNoCache(classOrObject),
                KotlinModificationTrackerService.getInstance(classOrObject.project).outOfBlockModificationTracker,
            )
        }

    override fun createFacade(
        project: Project,
        facadeClassFqName: FqName,
        searchScope: GlobalSearchScope,
    ): KtLightClassForFacade? = FacadeCache.getInstance(project)[facadeClassFqName, searchScope]

    override fun createFacadeForSyntheticFile(facadeClassFqName: FqName, file: KtFile): KtLightClassForFacade {
        val project = file.project
        val manager = PsiManager.getInstance(project)
        return LightClassGenerationSupport.getInstance(project).run {
            createUltraLightClassForFacade(manager, facadeClassFqName, listOf(file)) ?: error { "Unable to create UL class for facade" }
        }
    }

    override fun createScript(script: KtScript): KtLightClassForScript? = CachedValuesManager.getCachedValue(script) {
        CachedValueProvider.Result.create(
            createScriptNoCache(script),
            KotlinModificationTrackerService.getInstance(script.project).outOfBlockModificationTracker,
        )
    }

    companion object {
        fun createScriptNoCache(script: KtScript): KtLightClassForScript? {
            val containingFile = script.containingFile
            if (containingFile is KtCodeFragment) {
                // Avoid building light classes for code fragments
                return null
            }

            return LightClassGenerationSupport.getInstance(script.project).run {
                createUltraLightClassForScript(script)
            }
        }

        fun createClassNoCache(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration? {
            val containingFile = classOrObject.containingFile
            if (containingFile is KtCodeFragment) {
                // Avoid building light classes for code fragments
                return null
            }

            if (classOrObject.shouldNotBeVisibleAsLightClass()) {
                return null
            }

            return LightClassGenerationSupport.getInstance(classOrObject.project).run {
                createUltraLightClass(classOrObject)
            }
        }

        fun createFacadeNoCache(fqName: FqName, searchScope: GlobalSearchScope, project: Project): KtLightClassForFacade? {
            val sources = KotlinAsJavaSupport.getInstance(project)
                .findFilesForFacade(fqName, searchScope)
                .filterNot { it.isCompiled || it.isScript() }

            if (sources.isEmpty()) return null
            val manager = PsiManager.getInstance(project)
            return LightClassGenerationSupport.getInstance(project).run {
                if (!canCreateUltraLightClassForFacade(sources)) return null
                createUltraLightClassForFacade(manager, fqName, sources)
                    ?: error("Unable to create UL class for facade: $fqName for ${sources.joinToString { it.virtualFilePath }}")
            }
        }
    }
}