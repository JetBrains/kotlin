/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.LibraryModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.analyzer.SdkInfoBase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.idea.caches.project.ModuleProductionSourceScope
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.DeclarationProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.KtPackageProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveStateConfigurator
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtElement
import java.nio.file.Path
import java.nio.file.Paths

class FirModuleResolveStateConfiguratorIdeImpl(private val project: Project) : FirModuleResolveStateConfigurator() {
    override fun createPackagePartsProvider(scope: GlobalSearchScope): PackagePartProvider =
        IDEPackagePartProvider(scope)

    override fun createModuleDataProvider(moduleInfo: ModuleSourceInfoBase): ModuleDataProvider {
        return DependencyListForCliModule.build(
            moduleInfo.name,
            moduleInfo.platform,
            moduleInfo.analyzerServices
        ) {
            dependencies(moduleInfo.dependenciesWithoutSelf().extractLibraryPaths(project))
            friendDependencies(moduleInfo.modulesWhoseInternalsAreVisible().extractLibraryPaths(project))
            dependsOnDependencies(moduleInfo.expectedBy.extractLibraryPaths(project))
        }.moduleDataProvider
    }

    private fun Sequence<ModuleInfo>.extractLibraryPaths(project: Project): List<Path> {
        return fold(mutableListOf()) { acc, moduleInfo ->
            moduleInfo.extractLibraryPaths(acc)
            acc
        }
    }

    private fun Iterable<ModuleInfo>.extractLibraryPaths(project: Project): List<Path> {
        return fold(mutableListOf()) { acc, moduleInfo ->
            moduleInfo.extractLibraryPaths(acc)
            acc
        }
    }

    private fun ModuleInfo.extractLibraryPaths(destination: MutableList<Path>) {
        when (this) {
            is SdkInfoBase -> {
                val sdk = (this as SdkInfo).sdk
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).mapNotNullTo(destination) {
                    Paths.get(it.fileSystem.extractPresentableUrl(it.path)).normalize()
                }
            }
            is LibraryModuleInfo -> {
                getLibraryRoots().mapTo(destination) {
                    Paths.get(it).normalize()
                }
            }
        }
    }

    override fun getLanguageVersionSettings(moduleInfo: ModuleSourceInfoBase): LanguageVersionSettings {
        require(moduleInfo is ModuleSourceInfo)
        return moduleInfo.module.languageVersionSettings
    }

    override fun getModuleSourceScope(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        require(moduleInfo is ModuleSourceInfo)
        return ModuleProductionSourceScope(moduleInfo.module)
    }

    override fun createScopeForModuleLibraries(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        require(moduleInfo is ModuleSourceInfo)
        return ModuleLibrariesSearchScope(moduleInfo.module)
    }

    override fun createSealedInheritorsProvider(): SealedClassInheritorsProvider {
        return SealedClassInheritorsProviderIdeImpl()
    }

    override fun getModuleInfoFor(element: KtElement): ModuleInfo {
        return element.getModuleInfo()
    }

    override fun configureSourceSession(session: FirSession) {
    }
}