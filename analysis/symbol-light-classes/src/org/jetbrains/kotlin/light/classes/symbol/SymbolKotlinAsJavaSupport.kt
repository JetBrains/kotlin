/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.modification.createProjectWideLibraryModificationTracker
import org.jetbrains.kotlin.analysis.api.platform.modification.createProjectWideSourceModificationTracker
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiled.light.classes.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolBasedFakeLightClass
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForFacade
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForScript
import org.jetbrains.kotlin.light.classes.symbol.classes.createSymbolLightClassNoCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import java.util.*

private val KMP_CACHE: ThreadLocal<MutableMap<KtElement, KtLightClass?>> = ThreadLocal.withInitial { null }

private val isMultiplatformSupportAvailable: Boolean
    get() = KMP_CACHE.get() != null

/**
 * Enables light classes in non-JVM modules inside the given [block].
 *
 * The provided light classes might not correctly represent non-JVM concepts.
 * E.g., while class types provide qualified class names, [com.intellij.psi.impl.source.PsiClassReferenceType.resolve] might return
 * `false`, as in non-JVM modules there is usually no configured JDK.
 *
 * The method is designed to be used only for UAST (see https://plugins.jetbrains.com/docs/intellij/uast.html) in Android Lint.
 */
@KaNonPublicApi
@RequiresReadLock
fun <T> withMultiplatformLightClassSupport(project: Project, block: () -> T): T {
    if (isMultiplatformSupportAvailable) {
        // Allow reentrant access
        return block()
    }

    val permissionChecker = KaAnalysisPermissionChecker.getInstance(project)
    check(permissionChecker.isAnalysisAllowed()) {
        val rejectionReason = permissionChecker.getRejectionReason()
        "Cannot enable multiplatform light class support. $rejectionReason"
    }

    try {
        KMP_CACHE.set(WeakHashMap())
        return block()
    } finally {
        KMP_CACHE.set(null)
    }
}

@Deprecated(
    "Use withMultiplatformLightClassSupport(project, block) instead",
    ReplaceWith("withMultiplatformLightClassSupport(project, block)")
)
@KaNonPublicApi
@RequiresReadLock
fun <T> withMultiplatformLightClassSupport(block: () -> T): T {
    if (isMultiplatformSupportAvailable) {
        // Allow reentrant access
        return block()
    }

    require(ApplicationManager.getApplication().isReadAccessAllowed) { "The method can only run inside a read action" }
    require(!ApplicationManager.getApplication().isWriteAccessAllowed) { "The method cannot be run inside a write action" }

    try {
        KMP_CACHE.set(WeakHashMap())
        return block()
    } finally {
        KMP_CACHE.set(null)
    }
}

private fun KaModule.isLightClassSupportAvailable(): Boolean {
    return targetPlatform.has<JvmPlatform>() || isMultiplatformSupportAvailable
}

internal class SymbolKotlinAsJavaSupport(project: Project) : KotlinAsJavaSupportBase<KaModule>(project) {
    @KaCachedService
    private val projectStructureProvider by lazyPub { KotlinProjectStructureProvider.getInstance(project) }

    private data class CompiledFacadeFiles(
        val declarationFiles: List<KtClsFile>,
        val representativeFile: KtClsFile?,
    ) {
        val allFiles: List<KtFile>
            get() = buildList {
                addAll(declarationFiles)
                representativeFile?.takeUnless(declarationFiles::contains)?.let(::add)
            }

        val anchorFile: KtClsFile?
            get() = declarationFiles.firstOrNull() ?: representativeFile
    }

    private fun PsiElement.getModuleIfSupportEnabled(): KaModule? {
        return projectStructureProvider.getModule(
            element = this,
            useSiteModule = null,
        ).takeIf(KaModule::isLightClassSupportAvailable)
    }

    private fun collectCompiledFacadeFiles(
        facadeFqName: FqName,
        searchScope: GlobalSearchScope,
    ): CompiledFacadeFiles? {
        val declarationProvider = project.createDeclarationProvider(searchScope, contextualModule = null)
        val candidates = (declarationProvider.findInternalFilesForFacade(facadeFqName) + declarationProvider.findFilesForFacade(facadeFqName))
            .filterIsInstance<KtClsFile>()
            .distinctBy(KtFile::virtualFilePath)
            .mapNotNull { file ->
                file.compiledFacadeFileInfo()?.takeIf { it.facadeFqName == facadeFqName }?.let { file to it.kind }
            }

        if (candidates.isEmpty()) return null

        val declarationFiles = candidates.filter { it.second.isDeclarationFile }.map { it.first }
        val representativeFile = candidates.firstOrNull { !it.second.isDeclarationFile }?.first
            ?: candidates.firstOrNull { it.second == CompiledFacadeKind.FILE_FACADE }?.first
            ?: declarationFiles.firstOrNull()

        return CompiledFacadeFiles(
            declarationFiles = declarationFiles.ifEmpty { listOfNotNull(representativeFile) },
            representativeFile = representativeFile,
        )
    }

    private fun createCompiledLightFacade(
        facadeFqName: FqName,
        module: KaModule,
        files: CompiledFacadeFiles,
    ): KtLightClassForFacade? {
        if (files.allFiles.none(KtFile::hasTopLevelCallables)) return null
        return SymbolLightClassForFacade(facadeFqName, files.allFiles, module)
    }

    private fun KtClassOrObject.isSyntheticCompiledMultifilePartClass(): Boolean {
        val info = containingKtFile.compiledFacadeFileInfo() ?: return false
        return containingKtFile.isCompiled &&
                info.kind == CompiledFacadeKind.MULTIFILE_CLASS_PART &&
                parent == containingKtFile
    }

    override fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> = project.createDeclarationProvider(searchScope, contextualModule = null).run {
        getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName).flatMap {
            getAllClassesByClassId(ClassId.topLevel(packageFqName.child(it)))
        }
    }

    override fun findFilesForPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> = buildSet {
        addAll(project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacadeByPackage(packageFqName))
        findClassOrObjectDeclarationsInPackage(packageFqName, searchScope).mapTo(this) {
            it.containingKtFile
        }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacadeByPackage(packageFqName)
    }

    override fun findFilesForScript(scriptFqName: FqName, searchScope: GlobalSearchScope): Collection<KtScript> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForScript(scriptFqName)
    }

    private fun FqName.toClassIdSequence(): Sequence<ClassId> {
        var currentName = shortNameOrSpecial()
        if (currentName.isSpecial) return emptySequence()
        var currentParent = parentOrNull() ?: return emptySequence()
        var currentRelativeName = currentName.asString()

        return sequence {
            while (true) {
                yield(ClassId(currentParent, FqName(currentRelativeName), isLocal = false))
                currentName = currentParent.shortNameOrSpecial()
                if (currentName.isSpecial) break
                currentParent = currentParent.parentOrNull() ?: break
                currentRelativeName = "${currentName.asString()}.$currentRelativeName"
            }
        }
    }

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        val declarationProvider = project.createDeclarationProvider(searchScope, contextualModule = null)
        return fqName.toClassIdSequence()
            .flatMap(declarationProvider::getAllClassesByClassId)
            .filter { it.isFromSourceOrLibraryBinary() }
            .toSet()
    }

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean =
        project.createPackageProvider(scope).doesKotlinOnlyPackageExist(fqName)

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> =
        project.createPackageProvider(scope)
            .getKotlinOnlySubpackageNames(fqn)
            .map { fqn.child(it) }

    override fun createInstanceOfLightScript(script: KtScript): KtLightClass? {
        val module = script.getModuleIfSupportEnabled() ?: return null
        return SymbolLightClassForScript(script, module)
    }

    override fun KtFile.findModule(): KaModule? = getModuleIfSupportEnabled()

    override fun declarationLocation(file: KtFile): DeclarationLocation? = when (file.getModuleIfSupportEnabled()) {
        is KaSourceModule -> DeclarationLocation.ProjectSources
        is KaLibraryModule -> DeclarationLocation.LibraryClasses
        is KaLibrarySourceModule -> DeclarationLocation.LibrarySources
        else -> null
    }

    override fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        if (project.areSymbolLightClassesEnabledForLibraries()) {
            return createInstanceOfLightClass(classOrObject)
        }

        val lightClass = DecompiledLightClassesFactory.getLightClassForDecompiledClassOrObject(classOrObject, project)
        if (lightClass != null) {
            return lightClass
        }

        if (isMultiplatformSupportAvailable) {
            // Light classes for binary declarations are built over decompiled Java stubs which KMP files don't provide
            return createInstanceOfLightClass(classOrObject)
        }

        return null
    }

    override fun createInstanceOfLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        if (project.areSymbolLightClassesEnabledForLibraries() && classOrObject.isSyntheticCompiledMultifilePartClass()) {
            return null
        }

        val module = classOrObject.getModuleIfSupportEnabled() ?: return null
        return createSymbolLightClassNoCache(classOrObject, module)
    }

    override fun createInstanceOfDecompiledLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade? {
        if (project.areSymbolLightClassesEnabledForLibraries()) {
            return createInstanceOfLightFacade(facadeFqName, files)
        }

        val lightClass = DecompiledLightClassesFactory.createLightFacadeForDecompiledKotlinFile(project, facadeFqName, files)
        if (lightClass != null) {
            return lightClass
        }

        if (isMultiplatformSupportAvailable) {
            // Light classes for binary declarations are built over decompiled Java stubs which KMP files don't provide
            return createInstanceOfLightFacade(facadeFqName, files)
        }

        return null
    }

    override fun projectWideOutOfBlockModificationTracker(): ModificationTracker {
        return project.createProjectWideSourceModificationTracker()
    }

    override fun outOfBlockModificationTracker(element: PsiElement): ModificationTracker {
        return project.createProjectWideSourceModificationTracker()
    }

    override fun librariesTracker(element: PsiElement): ModificationTracker {
        return project.createProjectWideLibraryModificationTracker()
    }

    override fun getLightFacade(file: KtFile): KtLightClassForFacade? {
        if (!file.isValid || !project.areSymbolLightClassesEnabledForLibraries()) {
            return super.getLightFacade(file)
        }

        val clsFile = file as? KtClsFile ?: return super.getLightFacade(file)
        val facadeInfo = clsFile.compiledFacadeFileInfo() ?: return super.getLightFacade(file)
        val module = file.findModule() as? KaLibraryModule ?: return super.getLightFacade(file)
        if (facadeInfo.kind == CompiledFacadeKind.MULTIFILE_CLASS) return null

        return cacheLightClass(file) {
            val compiledFacadeFiles = collectCompiledFacadeFiles(facadeInfo.facadeFqName, module.contentSearchScope)
            val value = compiledFacadeFiles?.let { createCompiledLightFacade(facadeInfo.facadeFqName, module, it) }
            CachedValueProvider.Result.createSingleDependency(value, librariesTracker(file))
        }
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        if (!project.areSymbolLightClassesEnabledForLibraries()) {
            return super.getFacadeClasses(facadeFqName, scope)
        }

        val compiledFacadeFiles = collectCompiledFacadeFiles(facadeFqName, scope)
        if (compiledFacadeFiles != null) {
            val module = compiledFacadeFiles.anchorFile?.findModule() as? KaLibraryModule
            return listOfNotNull(module?.let { createCompiledLightFacade(facadeFqName, it, compiledFacadeFiles) })
        }

        val compiledCandidates = project.createDeclarationProvider(scope, contextualModule = null)
            .findFilesForFacade(facadeFqName)
            .filterIsInstance<KtClsFile>()
        if (compiledCandidates.isNotEmpty()) {
            return emptyList()
        }

        return super.getFacadeClasses(facadeFqName, scope)
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade? {
        val module = files.first().getModuleIfSupportEnabled()
        if (module != null) {
            val lightClass = createInstanceOfLightFacade(facadeFqName, module, files)
            if (lightClass != null) {
                return lightClass
            }
        }

        return null
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, module: KaModule, files: List<KtFile>): KtLightClassForFacade? {
        return SymbolLightClassForFacade(facadeFqName, files, module)
    }

    override val KaModule.contentSearchScope: GlobalSearchScope
        get() = GlobalSearchScope.union(
            buildList {
                add(contentScope)
                for (dependency in transitiveDependsOnDependencies) {
                    add(dependency.contentScope)
                }
            }
        )

    override fun facadeIsApplicable(module: KaModule, file: KtFile): Boolean =
        module.isFromSourceOrLibraryBinary() && module.isLightClassSupportAvailable()

    override fun getKotlinInternalClasses(fqName: FqName, scope: GlobalSearchScope): Collection<PsiClass> {
        val facadeKtFiles = project.createDeclarationProvider(scope, null).findInternalFilesForFacade(fqName)
        if (facadeKtFiles.isEmpty()) return emptyList()

        val partShortName = fqName.shortName().asString()
        val partClassFileShortName = "$partShortName.class"

        return facadeKtFiles.mapNotNull { facadeKtFile ->
            if (facadeKtFile is KtClsFile) {
                val partClassFile = facadeKtFile.virtualFile.parent.findChild(partClassFileShortName) ?: return@mapNotNull null
                val psiFile = facadeKtFile.manager.findFile(partClassFile) as? KtClsFile ?: facadeKtFile
                val javaClsClass = DecompiledLightClassesFactory.createClsJavaClassFromVirtualFile(
                    mirrorFile = psiFile,
                    classFile = partClassFile,
                    correspondingClassOrObject = null,
                    project = project,
                ) ?: return@mapNotNull null

                KtLightClassForDecompiledDeclaration(javaClsClass, javaClsClass.parent, psiFile, null)
            } else {
                null
            }
        }
    }

    override fun findFilesForFacade(facadeFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacade(facadeFqName)
    }

    override fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass = SymbolBasedFakeLightClass(classOrObject)

    override fun <E : KtElement, R : KtLightClass> cacheLightClass(element: E, provider: CachedValueProvider<R>): R? {
        return if (isMultiplatformSupportAvailable) {
            @Suppress("UNCHECKED_CAST")
            KMP_CACHE.get().computeIfAbsent(element) { provider.compute()?.value } as R?
        } else {
            super.cacheLightClass(element, provider)
        }
    }

    private fun KtElement.isFromSourceOrLibraryBinary(): Boolean = getModuleIfSupportEnabled()?.isFromSourceOrLibraryBinary() == true

    private fun KaModule.isFromSourceOrLibraryBinary(): Boolean {
        return when (this) {
            is KaSourceModule -> true
            is KaLibraryModule -> true
            is KaDanglingFileModule -> contextModule.isFromSourceOrLibraryBinary()
            else -> false
        }
    }
}
