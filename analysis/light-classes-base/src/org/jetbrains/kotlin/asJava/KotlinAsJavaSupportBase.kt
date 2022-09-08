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
import org.jetbrains.kotlin.psi.*

abstract class KotlinAsJavaSupportBase<TModule>(protected val project: Project) : KotlinAsJavaSupport() {
    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightFacade(file: KtFile): Pair<KtLightClassForFacade?, ModificationTracker>? {
        if (!file.facadeIsPossible()) return null

        val module = file.findModule().takeIf { facadeIsApplicable(it, file) } ?: return null
        val facadeFqName = file.javaFileFacadeFqName
        val facadeFiles = if (file.canHaveAdditionalFilesInFacade()) {
            findFilesForFacade(facadeFqName, module.contentSearchScope).filter(KtFile::isJvmMultifileClassFile)
        } else {
            listOf(file)
        }

        return when {
            facadeFiles.none(KtFile::hasTopLevelCallables) -> null
            facadeFiles.none(KtFile::isCompiled) -> {
                createInstanceOfLightFacade(facadeFqName, facadeFiles) to outOfBlockModificationTracker(file)
            }

            facadeFiles.all(KtFile::isCompiled) -> {
                createInstanceOfDecompiledLightFacade(facadeFqName, facadeFiles) to librariesTracker(file)
            }

            else -> error("Source and compiled files are mixed: $facadeFiles")
        }
    }

    /**
     * lightweight applicability check
     */
    private fun KtFile.facadeIsPossible(): Boolean = when {
        isCompiled && !name.endsWith(".class") -> false
        isScript() -> false
        canHaveAdditionalFilesInFacade() -> true
        else -> hasTopLevelCallables()
    }

    private fun KtFile.canHaveAdditionalFilesInFacade(): Boolean = !isCompiled && isJvmMultifileClassFile

    protected abstract fun KtFile.findModule(): TModule
    protected abstract fun facadeIsApplicable(module: TModule, file: KtFile): Boolean
    protected abstract val TModule.contentSearchScope: GlobalSearchScope

    protected abstract fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade
    protected abstract fun createInstanceOfDecompiledLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade?

    protected open fun projectWideOutOfBlockModificationTracker(): ModificationTracker {
        return KotlinModificationTrackerService.getInstance(project).outOfBlockModificationTracker
    }

    protected open fun outOfBlockModificationTracker(element: PsiElement): ModificationTracker {
        return projectWideOutOfBlockModificationTracker()
    }

    protected abstract fun librariesTracker(element: PsiElement): ModificationTracker

    override fun getLightFacade(file: KtFile): KtLightClassForFacade? = ifValid(file) {
        CachedValuesManager.getCachedValue(file) {
            val (facade, tracker) = createLightFacade(file) ?: (null to projectWideOutOfBlockModificationTracker())
            CachedValueProvider.Result.createSingleDependency(facade, tracker)
        }
    }

    override fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade {
        return createInstanceOfLightFacade(file.javaFileFacadeFqName, listOf(file))
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacadeByPackage(packageFqName, scope).toFacadeClasses()
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacade(facadeFqName, scope).toFacadeClasses()
    }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        return findFilesForFacadeByPackage(packageFqName, scope).mapNotNullTo(mutableSetOf()) { file ->
            file.takeIf { it.facadeIsPossible() }
                ?.takeIf { facadeIsApplicable(it.findModule(), file) }
                ?.javaFileFacadeFqName
                ?.shortName()
                ?.asString()
        }.toSet()
    }

    private fun Collection<KtFile>.toFacadeClasses(): List<KtLightClassForFacade> = filter {
        it.facadeIsPossible()
    }.groupBy {
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

    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightClass(classOrObject: KtClassOrObject): Pair<KtLightClass?, ModificationTracker>? {
        if (classOrObject.shouldNotBeVisibleAsLightClass()) return null

        val containingFile = classOrObject.containingKtFile
        when (declarationLocation(containingFile)) {
            DeclarationLocation.ProjectSources -> {
                return createInstanceOfLightClass(classOrObject) to outOfBlockModificationTracker(classOrObject)
            }

            DeclarationLocation.LibraryClasses -> {
                return createInstanceOfDecompiledLightClass(classOrObject) to librariesTracker(classOrObject)
            }

            DeclarationLocation.LibrarySources -> {
                val originalClassOrObject = ApplicationManager.getApplication()
                    .getService(KotlinDeclarationNavigationPolicy::class.java)
                    ?.getOriginalElement(classOrObject) as? KtClassOrObject

                val value = originalClassOrObject?.takeUnless(classOrObject::equals)?.let {
                    guardedRun { getLightClass(it) }
                }

                return value to librariesTracker(classOrObject)
            }

            null -> Unit
        }

        if (containingFile.analysisContext != null || containingFile.originalFile.virtualFile != null) {
            return createInstanceOfLightClass(classOrObject) to outOfBlockModificationTracker(classOrObject)
        }

        return null
    }

    protected abstract fun createInstanceOfLightClass(classOrObject: KtClassOrObject): KtLightClass?
    protected abstract fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject): KtLightClass?
    protected abstract fun declarationLocation(file: KtFile): DeclarationLocation?

    protected enum class DeclarationLocation {
        ProjectSources, LibraryClasses, LibrarySources,
    }

    override fun getLightClass(classOrObject: KtClassOrObject): KtLightClass? = ifValid(classOrObject) {
        CachedValuesManager.getCachedValue(classOrObject) {
            val (clazz, tracker) = createLightClass(classOrObject) ?: (null to projectWideOutOfBlockModificationTracker())
            CachedValueProvider.Result.createSingleDependency(clazz, tracker)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightScript(script: KtScript): Pair<KtLightClass?, ModificationTracker>? {
        val containingFile = script.containingFile
        if (containingFile is KtCodeFragment) {
            // Avoid building light classes for code fragments
            return null
        }

        return createInstanceOfLightScript(script) to projectWideOutOfBlockModificationTracker()
    }

    protected abstract fun createInstanceOfLightScript(script: KtScript): KtLightClass?

    override fun getLightClassForScript(script: KtScript): KtLightClass? = ifValid(script) {
        CachedValuesManager.getCachedValue(script) {
            val (lightScript, tracker) = createLightScript(script) ?: (null to projectWideOutOfBlockModificationTracker())
            CachedValueProvider.Result.createSingleDependency(lightScript, tracker)
        }
    }
}

private inline fun <T : PsiElement, V> ifValid(element: T, action: () -> V?): V? {
    if (!element.isValid) return null
    return action()
}