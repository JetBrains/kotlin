/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

/**
 * A shared base class for K1 [KotlinAsJavaSupport] implementations.
 *
 * Provides a user-data caching logic for light classes.
 * The user-data is stored in the corresponding [KtElement]s.
 */
abstract class KotlinAsJavaSupportK1Base<TModule : Any>(project: Project) : KotlinAsJavaSupportBase<TModule>(project) {
    override fun getLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass? = getLightClass(classOrObject)

    override fun getLightClass(classOrObject: KtClassOrObject, searchScope: GlobalSearchScope?): KtLightClass? = ifValid(classOrObject) {
        cacheLightClass(classOrObject) {
            val lightClass = createLightClass(classOrObject, null) ?: return@cacheLightClass cachedValueResult(null)
            val containingFile = classOrObject.containingKtFile
            val cachedValue = when (declarationLocation(containingFile)) {
                DeclarationLocation.ProjectSources -> {
                    LightClassCachedValue(lightClass, outOfBlockModificationTracker(classOrObject))
                }
                DeclarationLocation.LibraryClasses, DeclarationLocation.LibrarySources -> {
                    LightClassCachedValue(lightClass, librariesTracker(classOrObject))
                }
                else if (containingFile.analysisContext != null || containingFile.originalFile.virtualFile != null) -> {
                    LightClassCachedValue(lightClass, outOfBlockModificationTracker(classOrObject))
                }
                else -> null
            }

            cachedValueResult(cachedValue)
        }
    }

    override fun getLightFacade(file: KtFile, module: TModule?): KtLightClassForFacade? = getLightFacade(file)

    override fun getLightFacade(file: KtFile, searchScope: GlobalSearchScope?): KtLightClassForFacade? = ifValid(file) {
        cacheLightClass(file) {
            val contextModule = file.findContextModule(searchScope) { module ->
                facadeIsApplicable(module, file)
            } ?: return@cacheLightClass cachedValueResult(null)
            val lightFacade = createLightFacade(file, contextModule) ?: return@cacheLightClass cachedValueResult(null)
            val facadeFiles = lightFacade.files
            val cachedValue = when {
                facadeFiles.none(KtFile::isCompiled) -> {
                    LightClassCachedValue(lightFacade, outOfBlockModificationTracker(file))
                }
                facadeFiles.all(KtFile::isCompiled) -> {
                    LightClassCachedValue(lightFacade, librariesTracker(file))
                }

                else -> error("Source and compiled files are mixed: $facadeFiles")
            }
            cachedValueResult(cachedValue)
        }
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, module: TModule, files: List<KtFile>): KtLightClassForFacade? {
        return createInstanceOfLightFacade(facadeFqName, files)
    }

    override fun getLightClassForScript(script: KtScript, searchScope: GlobalSearchScope?): KtLightClass? = ifValid(script) {
        cacheLightClass(script) {
            val lightScript = createLightScript(script, null) ?: return@cacheLightClass cachedValueResult(null)
            val cachedValue = LightClassCachedValue(lightScript, projectWideOutOfBlockModificationTracker())
            cachedValueResult(cachedValue)
        }
    }

    private fun <E : KtElement, R : KtLightClass> cacheLightClass(element: E, provider: CachedValueProvider<R>): R? {
        return CachedValuesManager.getCachedValue(element, provider)
    }

    private fun <T : KtLightClass> cachedValueResult(lightClassCachedValue: LightClassCachedValue<T>?): CachedValueProvider.Result<T> {
        val value = lightClassCachedValue?.value
        val tracker = lightClassCachedValue?.tracker ?: projectWideOutOfBlockModificationTracker()
        return CachedValueProvider.Result.createSingleDependency(value, tracker)
    }

    override fun KtElement.findContextModule(scope: GlobalSearchScope?, moduleFilter: (TModule) -> Boolean): TModule? {
        return getContainingModule().takeIf(moduleFilter)
    }

    private class LightClassCachedValue<T : KtLightClass>(val value: T?, val tracker: ModificationTracker)
}
