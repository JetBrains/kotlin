/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisAllowanceManager
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionNavigationTargetsProvider
import org.jetbrains.kotlin.analysis.providers.impl.declarationProviders.FileBasedKotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtModuleStructureInternals
import org.jetbrains.kotlin.analysis.project.structure.analysisExtensionFileContextModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import java.util.concurrent.ConcurrentHashMap

/**
 * Encapsulate all the work with the [KtResolveExtension] for the LL API.
 *
 * Caches generated [KtResolveExtensionFile]s, creates [KotlinDeclarationProvider], [KotlinPackageProvider], [FirSymbolNamesProvider] needed
 * for the [org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider].
 */
abstract class LLFirResolveExtensionTool : FirSessionComponent {
    abstract val declarationProvider: LLFirResolveExtensionToolDeclarationProvider
    abstract val packageProvider: KotlinPackageProvider
    abstract val packageFilter: LLFirResolveExtensionToolPackageFilter
    abstract val shadowedSearchScope: GlobalSearchScope
    internal abstract val symbolNamesProvider: FirSymbolNamesProvider
}

val FirSession.llResolveExtensionTool: LLFirResolveExtensionTool? by FirSession.nullableSessionComponentAccessor()

internal class LLFirNonEmptyResolveExtensionTool(
    session: LLFirSession,
    extensions: List<KtResolveExtension>,
) : LLFirResolveExtensionTool() {
    init {
        require(extensions.isNotEmpty())
    }

    private val fileProvider = LLFirResolveExtensionsFileProvider(extensions)

    override val packageFilter = LLFirResolveExtensionToolPackageFilter(extensions)

    override val declarationProvider: LLFirResolveExtensionToolDeclarationProvider =
        LLFirResolveExtensionToolDeclarationProvider(fileProvider, session.ktModule)

    override val packageProvider: KotlinPackageProvider = LLFirResolveExtensionToolPackageProvider(packageFilter)

    override val shadowedSearchScope by lazy {
        forbidAnalysis {
            GlobalSearchScope.union(extensions.mapTo(mutableSetOf()) { it.getShadowedScope() })
        }
    }

    override val symbolNamesProvider: FirSymbolNamesProvider = LLFirResolveExtensionToolSymbolNamesProvider(packageFilter, fileProvider)
}

private class LLFirResolveExtensionToolSymbolNamesProvider(
    private val packageFilter: LLFirResolveExtensionToolPackageFilter,
    private val fileProvider: LLFirResolveExtensionsFileProvider,
) : FirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String> = forbidAnalysis {
        packageFilter.getAllPackages().mapToSetOrEmpty(FqName::asString)
    }

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> = forbidAnalysis {
        if (!packageFilter.packageExists(packageFqName)) return emptySet()
        fileProvider.getFilesByPackage(packageFqName).flatMap { it.getTopLevelClassifierNames() }.toSet()
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = forbidAnalysis {
        if (!packageFilter.packageExists(packageFqName)) return emptySet()
        fileProvider.getFilesByPackage(packageFqName)
            .flatMapTo(mutableSetOf()) { it.getTopLevelCallableNames() }
    }

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean = forbidAnalysis {
        if (!packageFilter.packageExists(classId.packageFqName)) return false

        fileProvider.getFilesByPackage(classId.packageFqName)
            .any { it.mayHaveTopLevelClassifier(classId.getTopLevelShortClassName()) }
    }

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = forbidAnalysis {
        if (!packageFilter.packageExists(packageFqName)) return false

        fileProvider.getFilesByPackage(packageFqName)
            .any { it.mayHaveTopLevelCallable(name) }
    }
}

class LLFirResolveExtensionToolPackageFilter(
    private val extensions: List<KtResolveExtension>
) {
    private val packageSubPackages: Map<FqName, Set<Name>> by lazy {
        val packagesFromExtensions = forbidAnalysis {
            extensions.flatMapTo(mutableSetOf()) { it.getContainedPackages() }
        }
        createSubPackagesMapping(packagesFromExtensions)
    }

    fun getAllPackages(): Set<FqName> {
        return packageSubPackages.keys
    }

    fun getAllSubPackages(packageFqName: FqName): Set<Name> {
        return packageSubPackages[packageFqName].orEmpty()
    }

    fun packageExists(packageFqName: FqName): Boolean {
        return packageFqName in packageSubPackages
    }

    private fun createSubPackagesMapping(packages: Set<FqName>): Map<FqName, Set<Name>> {
        return buildMap<FqName, MutableSet<Name>> {
            for (packageName in packages) {
                collectAllSubPackages(packageName)
            }
        }
    }

    private fun MutableMap<FqName, MutableSet<Name>>.collectAllSubPackages(packageName: FqName) {
        var currentPackage = FqName.ROOT
        for (packagePart in packageName.pathSegments()) {
            getOrPut(currentPackage) { mutableSetOf<Name>() }.add(packagePart)
            currentPackage = currentPackage.child(packagePart)
        }
        putIfAbsent(currentPackage, mutableSetOf())
    }
}

class LLFirResolveExtensionToolDeclarationProvider internal constructor(
    private val extensionProvider: LLFirResolveExtensionsFileProvider,
    private val ktModule: KtModule,
) : KotlinDeclarationProvider() {

    private val extensionFileToDeclarationProvider: ConcurrentHashMap<KtResolveExtensionFile, FileBasedKotlinDeclarationProvider> =
        ConcurrentHashMap()

    fun getTopLevelCallables(): Sequence<KtCallableDeclaration> = sequence {
        forEachDeclarationOfType<KtCallableDeclaration> { callable ->
            yield(callable)
        }
    }

    fun getTopLevelClassifiers(): Sequence<KtClassLikeDeclaration> = sequence {
        forEachDeclarationOfType<KtClass> { classLike ->
            yield(classLike)
        }
    }

    fun getTopLevelCallableNames(): Sequence<Name> = sequence {
        forEachDeclarationOfType<KtCallableDeclaration> { callable ->
            callable.nameAsName?.let { yield(it) }
        }
    }

    fun getTopLevelClassifierNames(): Sequence<Name> = sequence {
        forEachDeclarationOfType<KtClassLikeDeclaration> { classLike ->
            classLike.nameAsName?.let { yield(it) }
        }
    }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? = forbidAnalysis {
        return getDeclarationProvidersByPackage(classId.packageFqName) { it.mayHaveTopLevelClassifier(classId.getTopLevelShortClassName()) }
            .firstNotNullOfOrNull { it.getClassLikeDeclarationByClassId(classId) }
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> = forbidAnalysis {
        return getDeclarationProvidersByPackage(classId.packageFqName) { it.mayHaveTopLevelClassifier(classId.getTopLevelShortClassName()) }
            .flatMapTo(mutableListOf()) { it.getAllClassesByClassId(classId) }
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> = forbidAnalysis {
        return getDeclarationProvidersByPackage(classId.packageFqName) { it.mayHaveTopLevelClassifier(classId.getTopLevelShortClassName()) }
            .flatMapTo(mutableListOf()) { it.getAllTypeAliasesByClassId(classId) }
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> = forbidAnalysis {
        return getDeclarationProvidersByPackage(packageFqName) { true }
            .flatMapTo(mutableSetOf()) { it.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName) }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> = forbidAnalysis {
        return getDeclarationProvidersByPackage(callableId.packageName) { it.mayHaveTopLevelCallable(callableId.callableName) }
            .flatMapTo(mutableListOf()) { it.getTopLevelProperties(callableId) }
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> = forbidAnalysis {
        return getDeclarationProvidersByPackage(callableId.packageName) { it.mayHaveTopLevelCallable(callableId.callableName) }
            .flatMapTo(mutableListOf()) { it.getTopLevelFunctions(callableId) }
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> = forbidAnalysis {
        return getDeclarationProvidersByPackage(callableId.packageName) { it.mayHaveTopLevelCallable(callableId.callableName) }
            .mapTo(mutableListOf()) { it.kotlinFile }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = forbidAnalysis {
        return extensionProvider.getFilesByPackage(packageFqName).flatMapTo(mutableSetOf()) { it.getTopLevelCallableNames() }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> = forbidAnalysis {
        return getDeclarationProvidersByPackage(packageFqName) { file ->
            file.getTopLevelCallableNames().isNotEmpty()
        }.mapTo(mutableListOf()) { it.kotlinFile }
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> = forbidAnalysis {
        if (facadeFqName.isRoot) return emptyList()
        val packageFqName = facadeFqName.parent()
        return getDeclarationProvidersByPackage(packageFqName) { file ->
            facadeFqName.shortName().asString() == PackagePartClassUtils.getFilePartShortName(file.getFileName())
        }
            .mapTo(mutableListOf()) { it.kotlinFile }
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> = forbidAnalysis {
        // no decompiled files here (see the `org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider.findInternalFilesForFacade` KDoc)
        return emptyList()
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> = forbidAnalysis {
        if (scriptFqName.isRoot) return emptyList()
        val packageFqName = scriptFqName.parent()
        return getDeclarationProvidersByPackage(packageFqName) { file ->
            scriptFqName.shortName() == NameUtils.getScriptNameForFile(file.getFileName())
        }
            .mapNotNullTo(mutableListOf()) { it.kotlinFile.script }
    }

    override fun computePackageNames(): Set<String>? =
        buildSet {
            extensionProvider.extensions.forEach { extension ->
                extension.getContainedPackages().forEach { fqName ->
                    add(fqName.asString())
                }
            }
        }

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false

    private inline fun getDeclarationProvidersByPackage(
        packageFqName: FqName,
        crossinline filter: (KtResolveExtensionFile) -> Boolean
    ): Sequence<FileBasedKotlinDeclarationProvider> = forbidAnalysis {
        return extensionProvider.getFilesByPackage(packageFqName)
            .filter { filter(it) }
            .map { createDeclarationProviderByFile(it) }
    }

    private fun createDeclarationProviderByFile(file: KtResolveExtensionFile): FileBasedKotlinDeclarationProvider = forbidAnalysis {
        return extensionFileToDeclarationProvider.getOrPut(file) {
            val factory = KtPsiFactory(
                ktModule.project,
                markGenerated = true,
                eventSystemEnabled = true // so every generated KtFile backed by some VirtualFile
            )
            val ktFile = createKtFile(
                factory,
                file.getFileName(),
                file.buildFileText(),
                file.createNavigationTargetsProvider(),
            )
            FileBasedKotlinDeclarationProvider(ktFile)
        }
    }


    @OptIn(KtModuleStructureInternals::class)
    private fun createKtFile(
        factory: KtPsiFactory,
        fileName: String,
        fileText: String,
        navigationTargetsProvider: KtResolveExtensionNavigationTargetsProvider
    ): KtFile {
        val ktFile = factory.createFile(fileName, fileText)
        val virtualFile = ktFile.virtualFile
        virtualFile.analysisExtensionFileContextModule = ktModule
        virtualFile.navigationTargetsProvider = navigationTargetsProvider
        return ktFile
    }


    private inline fun <reified D : KtDeclaration> forEachDeclarationOfType(action: (D) -> Unit) {
        for (file in extensionProvider.getAllFiles()) {
            val provider = createDeclarationProviderByFile(file)
            val ktFile = provider.kotlinFile
            for (declaration in ktFile.declarations) {
                if (declaration is D) {
                    action(declaration)
                }
            }
        }
    }
}

internal class LLFirResolveExtensionsFileProvider(
    val extensions: List<KtResolveExtension>,
) {
    fun getFilesByPackage(packageFqName: FqName): Sequence<KtResolveExtensionFile> = forbidAnalysis {
        return extensions
            .asSequence()
            .filter { packageFqName in it.getContainedPackages() }
            .flatMap { it.getKtFiles() }
            .filter { it.getFilePackageName() == packageFqName }
    }

    fun getAllFiles(): Sequence<KtResolveExtensionFile> = forbidAnalysis {
        return extensions
            .asSequence()
            .flatMap { it.getKtFiles() }
    }
}

private class LLFirResolveExtensionToolPackageProvider(
    private val packageFilter: LLFirResolveExtensionToolPackageFilter,
) : KotlinPackageProvider() {
    override fun doesPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean =
        doesKotlinOnlyPackageExist(packageFqName)

    override fun getSubPackageFqNames(packageFqName: FqName, platform: TargetPlatform, nameFilter: (Name) -> Boolean): Set<Name> =
        getKotlinOnlySubPackagesFqNames(packageFqName, nameFilter)

    override fun doesPlatformSpecificPackageExist(packageFqName: FqName, platform: TargetPlatform): Boolean = false

    override fun getPlatformSpecificSubPackagesFqNames(packageFqName: FqName, platform: TargetPlatform, nameFilter: (Name) -> Boolean) =
        emptySet<Name>()

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean =
        packageFilter.packageExists(packageFqName)

    override fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name> {
        val subPackageNames = packageFilter.getAllSubPackages(packageFqName)
        if (subPackageNames.isEmpty()) return emptySet()
        return subPackageNames.filterTo(mutableSetOf()) { nameFilter(it) }
    }
}

private fun ClassId.getTopLevelShortClassName(): Name {
    return Name.guessByFirstCharacter(relativeClassName.asString().substringBefore("."))
}

private fun KtResolveExtensionFile.mayHaveTopLevelClassifier(name: Name): Boolean {
    return name in getTopLevelClassifierNames()
}

private fun KtResolveExtensionFile.mayHaveTopLevelCallable(name: Name): Boolean {
    return name in getTopLevelCallableNames()
}

@KtModuleStructureInternals
public var VirtualFile.navigationTargetsProvider: KtResolveExtensionNavigationTargetsProvider?
        by UserDataProperty(Key.create("KT_RESOLVE_EXTENSION_NAVIGATION_TARGETS_PROVIDER"))

private inline fun <R> forbidAnalysis(action: () -> R): R {
    return KtAnalysisAllowanceManager.forbidAnalysisInside(KtResolveExtensionProvider::class.java.simpleName, action)
}