/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleConverter
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
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
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.light.classes.symbol.classes.*
import org.jetbrains.kotlin.light.classes.symbol.utils.SafeNestedNullableCaffeineCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import java.time.Duration
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

/**
 * Light classes should only be created in the context of some JVM module.
 * The only exception is when [isMultiplatformSupportAvailable] is `true`.
 * Then, a declaration-site module is always used as a context one.
 */
private fun KaModule.isValidContextModule(): Boolean {
    return targetPlatform.isJvm() || isMultiplatformSupportAvailable
}

internal class SymbolKotlinAsJavaSupport(project: Project) : KotlinAsJavaSupportBase<KaModule>(project) {

    init {
        project.analysisMessageBus.connect(project).subscribe(
            KotlinModificationEvent.TOPIC,
            KotlinModificationEventListener { event ->
                when (event) {
                    is KotlinModuleStateModificationEvent,
                    is KotlinModuleOutOfBlockModificationEvent,
                    KotlinGlobalModuleStateModificationEvent,
                    KotlinGlobalSourceModuleStateModificationEvent,
                    KotlinGlobalScriptModuleStateModificationEvent,
                    KotlinGlobalSourceOutOfBlockModificationEvent,
                        -> {
                        moduleBasedLightClassCache.invalidateAll()
                        calculatedContextModuleCache.invalidateAll()
                    }

                    is KotlinCodeFragmentContextModificationEvent -> {}
                }
            }
        )
    }

    // ============ LIGHT FACADES ============
    //region Light Facades

    override fun getLightFacade(file: KtFile, searchScope: GlobalSearchScope?): KtLightClassForFacade? = ifValid(file) {
        val kaModule = file.findContextModule(searchScope) {
            facadeIsApplicable(it, file)
        } ?: return null
        getLightFacade(file, kaModule)
    }

    override fun getLightFacade(file: KtFile, module: KaModule?): KtLightClassForFacade? = ifValid(file) {
        if (module == null) return null
        cacheLightClass(file, module) {
            createLightFacade(file, module)
        }
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>): KtLightClassForFacade? {
        val kaModule = files.first().findContextModule() ?: return null
        return createInstanceOfLightFacade(facadeFqName, kaModule, files)
    }

    override fun createInstanceOfDecompiledLightFacade(
        facadeFqName: FqName,
        module: KaModule,
        files: List<KtFile>
    ): KtLightClassForFacade? {
        val lightClass = DecompiledLightClassesFactory.createLightFacadeForDecompiledKotlinFile(project, facadeFqName, files)
        if (lightClass != null) {
            return lightClass
        }

        if (isMultiplatformSupportAvailable) {
            // Light classes for binary declarations are built over decompiled Java stubs which KMP files don't provide
            return createInstanceOfLightFacade(facadeFqName, module, files)
        }

        return null
    }

    override fun createInstanceOfLightFacade(facadeFqName: FqName, module: KaModule, files: List<KtFile>): KtLightClassForFacade {
        return SymbolLightClassForFacade(facadeFqName, files, module)
    }

    override fun facadeIsApplicable(module: KaModule, file: KtFile): Boolean = module.isFromSourceOrLibraryBinary()

    //endregion

    // ============ LIGHT SCRIPTS ============
    //region Light Scripts

    override fun getLightClassForScript(script: KtScript, searchScope: GlobalSearchScope?): KtLightClass? = ifValid(script) {
        val kaModule = script.findContextModule(searchScope) ?: return null
        cacheLightClass(script, kaModule) {
            createLightScript(script, kaModule)
        }
    }

    override fun createInstanceOfLightScript(script: KtScript, module: KaModule?): KtLightClass? {
        if (module == null) return null
        return SymbolLightClassForScript(script, module)
    }
    //endregion

    // ============ LIGHT CLASSES ============
    //region Light Classes

    override fun getLightClass(classOrObject: KtClassOrObject, searchScope: GlobalSearchScope?): KtLightClass? = ifValid(classOrObject) {
        val kaModule = classOrObject.findContextModule(searchScope) ?: return null
        getLightClass(classOrObject, kaModule)
    }

    override fun getLightClass(classOrObject: KtClassOrObject, module: KaModule?): KtLightClass? = ifValid(classOrObject) {
        if (module == null) return null
        cacheLightClass(classOrObject, module) {
            createLightClass(classOrObject, module)
        }
    }

    override fun getFakeLightClass(classOrObject: KtClassOrObject): KtFakeLightClass = SymbolBasedFakeLightClass(classOrObject)

    override fun createInstanceOfDecompiledLightClass(classOrObject: KtClassOrObject, module: KaModule?): KtLightClass? {
        val lightClass = DecompiledLightClassesFactory.getLightClassForDecompiledClassOrObject(classOrObject, project)
        if (lightClass != null) {
            return lightClass
        }

        if (isMultiplatformSupportAvailable) {
            // Light classes for binary declarations are built over decompiled Java stubs which KMP files don't provide
            return createInstanceOfLightClass(classOrObject, module)
        }

        return null
    }

    override fun createInstanceOfLightClass(classOrObject: KtClassOrObject, module: KaModule?): KtLightClass? {
        if (module == null) return null
        return createSymbolLightClassNoCache(classOrObject, module)
    }

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
    //endregion

    // ============ KT ELEMENTS SEARCH ============
    //region KtElements Search

    override fun findClassOrObjectDeclarations(fqName: FqName, searchScope: GlobalSearchScope): Collection<KtClassOrObject> {
        val declarationProvider = project.createDeclarationProvider(searchScope, contextualModule = null)
        return fqName.toClassIdSequence()
            .flatMap(declarationProvider::getAllClassesByClassId)
            .filter { it.isFromSourceOrLibraryBinary() }
            .toSet()
    }

    override fun findClassOrObjectDeclarationsInPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope
    ): Collection<KtClassOrObject> = project.createDeclarationProvider(searchScope, contextualModule = null).run {
        getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName).flatMap {
            getAllClassesByClassId(ClassId.topLevel(packageFqName.child(it)))
        }
    }
    //endregion

    // ============ PACKAGE SEARCH ============
    //region Package Search

    override fun packageExists(fqName: FqName, scope: GlobalSearchScope): Boolean =
        project.createPackageProvider(scope).doesKotlinOnlyPackageExist(fqName)

    override fun getSubPackages(fqn: FqName, scope: GlobalSearchScope): Collection<FqName> =
        project.createPackageProvider(scope)
            .getKotlinOnlySubpackageNames(fqn)
            .map { fqn.child(it) }
    //endregion

    // ============ FILE SEARCH ============
    //region File Search

    override fun findFilesForPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> = buildSet {
        addAll(project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacadeByPackage(packageFqName))
        findClassOrObjectDeclarationsInPackage(packageFqName, searchScope).mapTo(this) {
            it.containingKtFile
        }
    }

    override fun findFilesForFacade(facadeFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacade(facadeFqName)
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName, searchScope: GlobalSearchScope): Collection<KtFile> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForFacadeByPackage(packageFqName)
    }

    override fun findFilesForScript(scriptFqName: FqName, searchScope: GlobalSearchScope): Collection<KtScript> {
        return project.createDeclarationProvider(searchScope, contextualModule = null).findFilesForScript(scriptFqName)
    }
    //endregion

    // ============ TRACKERS AND UTILS ============
    //region Trackers and Utils
    /**
     * [getResolutionScope] is used to adjust the resolve scope of [FakeFileForLightClass] from common modules.
     * It's vital for issues like KT-71429 or KT-40059 when the consumed / returned type of declaration from a common module
     * is an `expect` class.
     *
     * When Java tries to resolve some type inside a declaration in a common module, it searches for the class declaration
     * in the resolve scope of the containing common file (i.e., passes this resolution scope to [JavaElementFinder]).
     * This scope is a resolution scope of the containing common module.
     * However, there might be several issues:
     * - The type is a user-defined `expect` class. In this case, Java will search for it in the common module and find just
     *   the `expect` declaration and not the `actual` one. Light classes are prohibited for `expect` classes, so the resolution will fail.
     *   In this case, we need to enlarge the resolve scope of the containing common file to include scopes of all JVM implementation modules,
     *   which might contain the corresponding `actual` declaration.
     * - The type is a built-in Kotlin `expect` class. One of such examples is `kotlin.Function`.
     *   Numbered actualizations (`FunctionN`) are platform-specific. So if some LC uses `kotlin.jvm.Function1`, Java won't find it
     *   in the resolve scope of the containing common module. In this case, we need to include all platform stdlibs in the resolve scope.
     *
     * [getResolutionScope] returns a union of resolution scopes of all modules represented by this file.
     * These modules are context modules of light classes taken from [FakeFileForLightClass.getClasses].
     * If no modules were found, returns the default common scope.
     *
     * Note that [getResolutionScope] doesn't do any target platform adjustments by itself.
     * It's expected that all classes created through [JavaElementFinder] are constructed with some JVM module as a context:
     * - If the original declaration was already in a JVM module, this module is used as a context.
     *   In this case, [getResolutionScope] just provides an identical scope.
     * - If the original declaration was in a common module, the first found JVM implementation module is passed as a context.
     *   In this case, [getResolutionScope] returns the scope of this implementation module.
     */
    override fun getResolutionScope(file: FakeFileForLightClass): GlobalSearchScope {
        val analysisScopesForContextModules = file.classes.mapNotNullTo(mutableSetOf()) { lightClass ->
            (lightClass as? SymbolLightClassBase)?.ktModule
        }.map { module ->
            analyzeForLightClasses(module) {
                analysisScope
            }
        }

        return when {
            analysisScopesForContextModules.isEmpty() -> super.getResolutionScope(file)
            analysisScopesForContextModules.size == 1 -> analysisScopesForContextModules.single()
            else -> GlobalSearchScope.union(analysisScopesForContextModules)
        }
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

    override fun declarationLocation(file: KtFile): DeclarationLocation? = when (file.getContainingModule()) {
        is KaSourceModule -> DeclarationLocation.ProjectSources
        is KaLibraryModule -> DeclarationLocation.LibraryClasses
        is KaLibrarySourceModule -> DeclarationLocation.LibrarySources
        else -> null
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

    override fun KtElement.getContainingModule(): KaModule {
        return projectStructureProvider.getModule(
            element = this,
            useSiteModule = null,
        )
    }

    @OptIn(KaIdeApi::class)
    override fun KtElement.findContextModule(scope: GlobalSearchScope?, moduleFilter: (KaModule) -> Boolean): KaModule? {
        val declarationModule = getContainingModule().takeIf(moduleFilter) ?: return null
        return calculatedContextModuleCache.getOrPut(declarationModule, scope) { declarationModule, scope ->
            findContextModuleNonCached(declarationModule, scope)
        }
    }

    @OptIn(KaIdeApi::class)
    private fun KtElement.findContextModuleNonCached(declarationModule: KaModule, scope: GlobalSearchScope?): KaModule? {
        val suitableImplementingDependents = KotlinModuleDependentsProvider.getInstance(project).getRefinementDependents(declarationModule)
            .filter { module ->
                module.isValidContextModule() && analyzeForLightClasses(module) {
                    // This is needed to handle production / test modules on the Intellij side.
                    // E.g., if the declaration-site module is commonTest with kind=TEST, then
                    // getRefinementDependents returns two jvmTest modules: one with kind=TEST and another with kind=PRODUCTION.
                    // We cannot blindly return PRODUCTION jvmTest module, as it doesn't depend on TEST commonTest module,
                    // only on PRODUCTION commonTest.
                    this@findContextModuleNonCached.canBeAnalysed()
                }
            }

        if (suitableImplementingDependents.isNotEmpty()) {
            val moduleConverter = KaModuleConverter.getInstance()

            if (scope == null || moduleConverter == null) {
                // If it's impossible to calculate the precise implementation module,
                // a random JVM implementation module is still better than a common one
                return suitableImplementingDependents.first()
            }


            val implementationModuleCoveredByScope = suitableImplementingDependents.firstNotNullOfOrNull { dependentModule ->
                val ideaModule = moduleConverter.asOpenApiModule(dependentModule) ?: return@firstNotNullOfOrNull null
                dependentModule.takeIf { scope.isSearchInModuleContent(ideaModule) }
            }

            if (implementationModuleCoveredByScope != null) {
                return implementationModuleCoveredByScope
            }
        }

        return declarationModule.takeIf(KaModule::isValidContextModule)
    }

    private fun KtElement.isFromSourceOrLibraryBinary(): Boolean =
        getContainingModule().isFromSourceOrLibraryBinary()

    private fun KaModule.isFromSourceOrLibraryBinary(): Boolean {
        return when (this) {
            is KaSourceModule -> true
            is KaLibraryModule -> true
            is KaDanglingFileModule -> contextModule.isFromSourceOrLibraryBinary()
            else -> false
        }
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

    @KaCachedService
    private val projectStructureProvider by lazyPub { KotlinProjectStructureProvider.getInstance(project) }
    //endregion

    // ============ CACHE ============
    //region Cache

    /**
     * Stores a map [KaModule] -> [KtElement] -> [KtLightClass].
     *
     * [KaModule] represents the module which is used as a context for the light class creation.
     *
     * The whole cache gets invalidated on every project modification.
     */
    private val moduleBasedLightClassCache = SafeNestedNullableCaffeineCache<KaModule, KtElement, KtLightClass>(
        outerCache =
            Caffeine.newBuilder()
                .weakKeys()
                .build(),
        innerCacheFactory = {
            Caffeine.newBuilder()
                .weakKeys()
                .softValues()
                .build()
        }
    )

    /**
     * Stores a map declaration-site [KaModule] -> [GlobalSearchScope] -> context [KaModule] found for [KaModule] in [GlobalSearchScope].
     *
     * The whole cache gets invalidated on every project modification.
     */
    private val calculatedContextModuleCache = SafeNestedNullableCaffeineCache<KaModule, GlobalSearchScope, KaModule>(
        outerCache =
            Caffeine.newBuilder()
                .weakKeys()
                .expireAfterAccess(Duration.ofSeconds(10))
                .build(),
        innerCacheFactory = {
            Caffeine.newBuilder()
                .weakKeys()
                .weakValues()
                .expireAfterAccess(Duration.ofSeconds(5))
                .build()
        }
    )

    private fun <R : KtLightClass> cacheLightClass(
        element: KtElement,
        module: KaModule,
        provider: () -> R?
    ): R? {
        val computedValue = if (isMultiplatformSupportAvailable) {
            KMP_CACHE.get().computeIfAbsent(element) { provider() }
        } else {
            moduleBasedLightClassCache.getOrPut(module, element) { _, _ ->
                provider()
            }
        }

        @Suppress("UNCHECKED_CAST")
        return computedValue as R?
    }
    //endregion
}
