/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.ResolveScopeManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.fileClasses.isJvmMultifileClassFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

abstract class KotlinAsJavaSupportBase<TModule : Any>(protected val project: Project) : KotlinAsJavaSupport() {
    // ============ LIGHT FACADES ============
    //region Light Facades

    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightFacade(file: KtFile, module: TModule): KtLightClassForFacade? {
        if (!file.facadeIsPossible()) return null

        val facadeFqName = file.javaFileFacadeFqName
        val facadeFiles = if (file.canHaveAdditionalFilesInFacade()) {
            findFilesForFacade(facadeFqName, module.contentSearchScope).filter(KtFile::isJvmMultifileClassFile)
        } else {
            listOf(file)
        }

        return when {
            facadeFiles.none(KtFile::hasTopLevelCallables) -> null
            facadeFiles.none(KtFile::isCompiled) -> {
                createInstanceOfLightFacade(facadeFqName, module, facadeFiles)
            }

            facadeFiles.all(KtFile::isCompiled) -> {
                createInstanceOfDecompiledLightFacade(facadeFqName, module, facadeFiles)
            }

            else -> error("Source and compiled files are mixed: $facadeFiles")
        }
    }

    override fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade {
        return createInstanceOfLightFacade(file.javaFileFacadeFqName, listOf(file)) ?: errorWithAttachment(
            "Unsupported ${file::class.simpleName}"
        ) {
            withEntry("module", file.getContainingModule().toString())
            withPsiEntry("file", file)
        }
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacade(facadeFqName, scope).toFacadeClasses(scope)
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacadeByPackage(packageFqName, scope).toFacadeClasses(scope)
    }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        return findFilesForFacadeByPackage(packageFqName, scope).mapNotNullTo(mutableSetOf()) { file ->
            file.takeIf { it.facadeIsPossible() }
                ?.takeIf { facadeIsApplicable(it.getContainingModule(), file) }
                ?.javaFileFacadeFqName
                ?.shortName()
                ?.asString()
        }.toSet()
    }

    private fun Collection<KtFile>.toFacadeClasses(scope: GlobalSearchScope): List<KtLightClassForFacade> = mapNotNull { file ->
        file.takeIf { it.facadeIsPossible() }?.findContextModule(scope) { facadeIsApplicable(it, file) }?.let { file to it }
    }.groupBy { (file, module) ->
        FacadeKey(file.javaFileFacadeFqName, file.isJvmMultifileClassFile, module)
    }.mapNotNull { (_, pairs) ->
        pairs.firstOrNull()?.let { (file, module) -> getLightFacade(file, module) }
    }

    private data class FacadeKey<TModule>(val fqName: FqName, val isMultifile: Boolean, val module: TModule)

    /**
     * lightweight applicability check
     */
    protected fun KtFile.facadeIsPossible(): Boolean = when {
        isCompiled && !name.endsWith(".class") -> false
        isScript() -> false
        canHaveAdditionalFilesInFacade() -> true
        else -> hasTopLevelCallables()
    }

    protected abstract fun facadeIsApplicable(module: TModule, file: KtFile): Boolean

    protected abstract fun getLightFacade(file: KtFile, module: TModule?): KtLightClassForFacade?
    protected abstract fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade?
    protected abstract fun createInstanceOfDecompiledLightFacade(
        facadeFqName: FqName,
        module: TModule,
        files: List<KtFile>,
    ): KtLightClassForFacade?

    protected abstract fun createInstanceOfLightFacade(facadeFqName: FqName, module: TModule, files: List<KtFile>): KtLightClassForFacade?

    private fun KtFile.canHaveAdditionalFilesInFacade(): Boolean = !isCompiled && isJvmMultifileClassFile
    //endregion

    // ============ LIGHT SCRIPTS ============
    //region Light Scripts

    fun createLightScript(script: KtScript, module: TModule?): KtLightClass? {
        val containingFile = script.containingFile
        if (containingFile is KtCodeFragment) {
            // Avoid building light classes for code fragments
            return null
        }

        return createInstanceOfLightScript(script, module)
    }

    override fun getScriptClasses(scriptFqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        if (scriptFqName.isRoot) {
            return emptyList()
        }

        return findFilesForScript(scriptFqName, scope).mapNotNull { getLightClassForScript(it, scope) }
    }

    protected abstract fun createInstanceOfLightScript(script: KtScript, module: TModule?): KtLightClass?
    //endregion

    // ============ LIGHT CLASSES ============
    //region Light Classes

    @Suppress("MemberVisibilityCanBePrivate")
    fun createLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass? {
        if (classOrObject.shouldNotBeVisibleAsLightClass()) return null

        val containingFile = classOrObject.containingKtFile
        when (declarationLocation(containingFile)) {
            DeclarationLocation.ProjectSources -> {
                return createInstanceOfLightClass(classOrObject, module)
            }

            DeclarationLocation.LibraryClasses -> {
                return createInstanceOfDecompiledLightClass(classOrObject, module)
            }

            DeclarationLocation.LibrarySources -> {
                val originalClassOrObject = ApplicationManager.getApplication()
                    .getService(KotlinDeclarationNavigationPolicy::class.java)
                    ?.getOriginalElement(classOrObject) as? KtClassOrObject

                val value = originalClassOrObject?.takeUnless(classOrObject::equals)?.let {
                    guardedRun { getLightClass(it, module) }
                }

                return value
            }

            null -> Unit
        }

        if (containingFile.analysisContext != null || containingFile.originalFile.virtualFile != null) {
            return createInstanceOfLightClass(classOrObject, module)
        }

        return null
    }

    protected abstract fun getLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass?
    protected abstract fun createInstanceOfLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass?
    protected abstract fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject, module: TModule?): KtLightClass?
    //endregion

    // ============ TRACKERS AND UTILS ============
    //region Trackers and Utils

    /**
     * Default implementation of resolution scope for [file] used by Java
     * to query [org.jetbrains.kotlin.asJava.finder.JavaElementFinder] with
     * when resolving declarations in this file.
     */
    override fun getResolutionScope(file: FakeFileForLightClass): GlobalSearchScope {
        return ResolveScopeManager.getInstance(project).getDefaultResolveScope(file.virtualFile)
    }

    abstract fun projectWideOutOfBlockModificationTracker(): ModificationTracker

    open fun outOfBlockModificationTracker(element: PsiElement): ModificationTracker {
        return projectWideOutOfBlockModificationTracker()
    }

    abstract fun librariesTracker(element: PsiElement): ModificationTracker

    /**
     * Returns a containing module for [this].
     */
    protected abstract fun KtElement.getContainingModule(): TModule

    /**
     * Returns a module covered by [scope] which should be used as a context for the light class creation for [this].
     *
     * [moduleFilter] is called on the containing module to ensure that this module is allowed for the light class creation.
     * If [moduleFilter] returns `false` on the containing module, returns `null`.
     * If [scope] is `null`, the containing module is returned.
     */
    protected abstract fun KtElement.findContextModule(
        scope: GlobalSearchScope? = null,
        moduleFilter: (TModule) -> Boolean = { true }
    ): TModule?

    protected abstract val TModule.contentSearchScope: GlobalSearchScope

    protected abstract fun declarationLocation(file: KtFile): DeclarationLocation?

    protected enum class DeclarationLocation {
        ProjectSources, LibraryClasses, LibrarySources,
    }

    protected inline fun <T : PsiElement, V> ifValid(element: T, action: () -> V?): V? {
        ProgressManager.checkCanceled()

        return if (!element.isValid)
            null
        else
            action()
    }

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
    //endregion

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinAsJavaSupportBase<*> {
            return KotlinAsJavaSupport.getInstance(project) as KotlinAsJavaSupportBase<*>
        }
    }
}

