/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions

import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.FileBasedKotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirSymbolProviderNameCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirSymbolProviderNameCacheBase
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtModuleStructureInternals
import org.jetbrains.kotlin.analysis.project.structure.analysisContextModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.KtResolveExtension
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

internal abstract class LLFirResolveExtensionTool : FirSessionComponent {
    abstract val modificationTrackers: List<ModificationTracker>
    abstract val declarationProvider: KotlinDeclarationProvider
    abstract val packageProvider: KotlinPackageProvider
    abstract val symbolNameCache: LLFirSymbolProviderNameCache
}

internal val FirSession.llResolveExtensionTool: LLFirResolveExtensionTool? by FirSession.nullableSessionComponentAccessor()

internal class LLFirNonEmptyResolveExtensionTool(
    private val session: LLFirSession,
    private val extensions: List<KtResolveExtension>,
) : LLFirResolveExtensionTool() {
    init {
        require(extensions.isNotEmpty())
    }

    private val packageFilter = LLFirResolveExtensionToolPackageFilter(extensions)

    override val modificationTrackers by lazy {
        extensions.map { it.getModificationTracker() }
    }

    override val declarationProvider: KotlinDeclarationProvider =
        LLFirResolveExtensionToolDeclarationProvider(extensions, session.ktModule, packageFilter)

    override val packageProvider: KotlinPackageProvider =
        LLFirResolveExtensionToolPackageProvider(packageFilter)

    override val symbolNameCache = object : LLFirSymbolProviderNameCacheBase(session) {
        override fun computeClassifierNames(packageFqName: FqName): Set<String>? {
            if (!packageFilter.packageExists(packageFqName)) return emptySet()
            return declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
                .mapTo(mutableSetOf()) { it.asString() }
        }

        override fun computeCallableNames(packageFqName: FqName): Set<Name>? {
            if (!packageFilter.packageExists(packageFqName)) return emptySet()
            return declarationProvider.getTopLevelCallableNamesInPackage(packageFqName)
        }
    }
}

private class LLFirResolveExtensionToolPackageFilter(
    private val extensions: List<KtResolveExtension>
) {
    val allPackages: Set<FqName> by lazy {
        buildSet {
            for (extension in extensions) {
                val packages = extension.getPackagesToBeResolved() ?: return@lazy null
                addAll(packages)
            }
        }
    }

    fun packageExists(packageFqName: FqName): Boolean {
        return packageFqName in allPackages
    }
}

private class LLFirResolveExtensionToolDeclarationProvider(
    extensions: List<KtResolveExtension>,
    private val ktModule: KtModule,
    private val packageFilter: LLFirResolveExtensionToolPackageFilter,
) : FileBasedKotlinDeclarationProvider() {
    override val ktFiles: List<KtFile> by lazy {
        val factory = KtPsiFactory(ktModule.project, markGenerated = true)
        buildList {
            for (extension in extensions) {
                for (file in extension.getKtFiles()) {
                    add(createKtFile(factory, file.fileName, file.text))
                }
            }
        }
    }

    @OptIn(KtModuleStructureInternals::class)
    private fun createKtFile(factory: KtPsiFactory, fileName: String, fileText: String): KtFile {
        val ktFile = factory.createFile(fileName, fileText)
        ktFile.virtualFile.analysisContextModule = ktModule
        return ktFile
    }

    override fun mightContainPackage(packageFqName: FqName): Boolean =
        packageFilter.packageExists(packageFqName)
}

private class LLFirResolveExtensionToolPackageProvider(
    private val packageFilter: LLFirResolveExtensionToolPackageFilter,
) : KotlinPackageProvider() {

    private val packageSubPackages: Map<FqName, Set<Name>> by lazy {
        createSubPackagesMapping(packageFilter.allPackages)
    }

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
        val subPackageNames = packageSubPackages[packageFqName] ?: return emptySet()
        if (subPackageNames.isEmpty()) return emptySet()
        return subPackageNames.filterTo(mutableSetOf()) { nameFilter(it) }
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
    }
}

