/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.fileClasses.isJvmMultifileClassFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KotlinDeclarationNavigationPolicy
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.analysisContext

abstract class KotlinAsJavaSupportBase<TModule>(protected val project: Project) : KotlinAsJavaSupport() {
    fun createLightFacade(file: KtFile): Pair<KtLightClassForFacade?, ModificationTracker>? {
        if (file.isScript()) return null
        if (file.isCompiled && !file.name.endsWith(".class")) return null

        val module = file.findModule().takeIf { facadeIsApplicable(it, file) } ?: return null
        val facadeFqName = file.javaFileFacadeFqName
        val facadeFiles = if (file.isJvmMultifileClassFile && !file.isCompiled) {
            findFilesForFacade(facadeFqName, module.contentSearchScope).filter(KtFile::isJvmMultifileClassFile)
        } else {
            listOf(file)
        }

        return when {
            facadeFiles.none(KtFile::hasTopLevelCallables) -> null
            facadeFiles.none(KtFile::isCompiled) -> {
                createInstanceOfLightFacade(facadeFqName, facadeFiles, module) to outOfBlockModificationTracker(file, module)
            }

            facadeFiles.all(KtFile::isCompiled) -> {
                createInstanceOfDecompiledLightFacade(facadeFqName, facadeFiles, module) to librariesTracker(file, module)
            }

            else -> error("Source and compiled files are mixed: $facadeFiles")
        }
    }

    protected abstract fun KtFile.findModule(): TModule
    protected abstract fun facadeIsApplicable(module: TModule, file: KtFile): Boolean
    protected abstract val TModule.contentSearchScope: GlobalSearchScope

    protected abstract fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>, module: TModule): KtLightClassForFacade
    protected abstract fun createInstanceOfDecompiledLightFacade(
        facadeFqName: FqName,
        files: List<KtFile>,
        module: TModule,
    ): KtLightClassForFacade?

    protected open fun outOfBlockModificationTracker(element: PsiElement, module: TModule): ModificationTracker {
        return projectWideOutOfBlockModificationTracker()
    }

    protected open fun librariesTracker(element: PsiElement, module: TModule): ModificationTracker {
        return projectWideOutOfBlockModificationTracker()
    }

    protected open fun projectWideOutOfBlockModificationTracker(): ModificationTracker {
        return KotlinModificationTrackerService.getInstance(project).outOfBlockModificationTracker
    }

    override fun getLightFacade(file: KtFile): KtLightClassForFacade? = ifValid(file) {
        CachedValuesManager.getCachedValue(file) {
            val (facade, tracker) = createLightFacade(file) ?: (null to projectWideOutOfBlockModificationTracker())
            CachedValueProvider.Result.createSingleDependency(facade, tracker)
        }
    }

    override fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade {
        return createInstanceOfLightFacade(file.javaFileFacadeFqName, listOf(file), file.findModule())
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacadeByPackage(packageFqName, scope).toFacadeClasses()
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacade(facadeFqName, scope).toFacadeClasses()
    }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        return findFilesForFacadeByPackage(packageFqName, scope).mapNotNullTo(mutableSetOf()) { file ->
            file.takeIf { facadeIsApplicable(it.findModule(), file) }?.javaFileFacadeFqName?.shortName()?.asString()
        }.toSet()
    }

    private fun Collection<KtFile>.toFacadeClasses(): List<KtLightClassForFacade> = groupBy {
        FacadeKey(it.javaFileFacadeFqName, it.isJvmMultifileClassFile, it.findModule())
    }.mapNotNull { (key, files) ->
        files.firstOrNull { facadeIsApplicable(key.module, it) }?.let(::getLightFacade)
    }

    private data class FacadeKey<TModule>(val fqName: FqName, val isMultifile: Boolean, val module: TModule)

    private val recursiveGuard = ThreadLocal<Boolean>()
    private inline fun <T> guardedRun(body: () -> T): T? {
        if (recursiveGuard.get() == true) return null
        return try {
            recursiveGuard.set(true)
            body()
        } finally {
            recursiveGuard.set(false)
        }
    }

    fun createLightClass(classOrObject: KtClassOrObject): Pair<KtLightClass?, ModificationTracker>? {
        if (classOrObject.shouldNotBeVisibleAsLightClass()) return null

        val containingFile = classOrObject.containingKtFile
        val module = containingFile.findModule()
        when (declarationLocation(containingFile, module)) {
            DeclarationLocation.ProjectSources -> {
                return createInstanceOfLightClass(classOrObject, module) to outOfBlockModificationTracker(classOrObject, module)
            }

            DeclarationLocation.LibraryClasses -> {
                return createInstanceOfDecompiledLightClass(classOrObject, module) to librariesTracker(classOrObject, module)
            }

            DeclarationLocation.LibrarySources -> {
                val originalClassOrObject = ApplicationManager.getApplication()
                    .getService(KotlinDeclarationNavigationPolicy::class.java)
                    ?.getOriginalElement(classOrObject) as? KtClassOrObject

                val value = originalClassOrObject?.takeUnless(classOrObject::equals)?.let {
                    guardedRun { getLightClass(it) }
                }

                return value to librariesTracker(classOrObject, module)
            }

            null -> Unit
        }

        if (containingFile.analysisContext != null || containingFile.originalFile.virtualFile != null) {
            return createInstanceOfLightClass(classOrObject, module) to outOfBlockModificationTracker(classOrObject, module)
        }

        return null
    }

    protected abstract fun createInstanceOfLightClass(classOrObject: KtClassOrObject, module: TModule): KtLightClass?
    protected abstract fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject, module: TModule): KtLightClass?
    protected abstract fun declarationLocation(file: KtFile, module: TModule): DeclarationLocation?

    protected enum class DeclarationLocation {
        ProjectSources, LibraryClasses, LibrarySources,
    }

    override fun getLightClass(classOrObject: KtClassOrObject): KtLightClass? = ifValid(classOrObject) {
        CachedValuesManager.getCachedValue(classOrObject) {
            val (clazz, tracker) = createLightClass(classOrObject) ?: (null to projectWideOutOfBlockModificationTracker())
            CachedValueProvider.Result.createSingleDependency(clazz, tracker)
        }
    }
}

private fun <T : PsiElement, V> ifValid(element: T, action: () -> V?): V? {
    if (!element.isValid) return null
    return action()
}