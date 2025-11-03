/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils.getVirtualFilesForLibraryRoots
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.hasFallbackDependencies
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.platform.isMultiPlatform
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import org.junit.Assume
import java.nio.file.Path

/**
 * @see TestModuleKind.LibrarySource
 */
object KtLibrarySourceTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        Assume.assumeFalse(
            "Compilation of multi-platform libraries is not supported",
            testModule.targetPlatform(testServices).isMultiPlatform(),
        )

        val (libraryJars, librarySourcesJars) = testServices.compiledLibraryProvider.compileToLibrary(testModule, dependencyBinaryRoots)

        require(librarySourcesJars.isNotEmpty())

        return createKtLibrarySourceModule(
            libraryJars = libraryJars,
            librarySourcesJars = librarySourcesJars,
            testModule = testModule,
            project = project,
            testServices = testServices,
        )
    }
}

@OptIn(KaImplementationDetail::class)
fun createKtLibrarySourceModule(
    libraryJars: List<Path>,
    librarySourcesJars: List<Path>,
    testModule: TestModule,
    project: Project,
    testServices: TestServices,
): KtTestModule {
    val targetPlatform = testModule.targetPlatform(testServices)
    val binaryRootsVirtualFiles =
        getVirtualFilesForLibraryRoots(libraryJars, testServices.environmentManager.getApplicationEnvironment())

    val libraryKtModule = object : KaLibraryModuleImpl(
        testModule.name,
        targetPlatform,
        StandaloneProjectFactory.createLibraryModuleSearchScope(
            binaryRootsVirtualFiles,
            project,
        ),
        project,
        binaryRoots = libraryJars,
        binaryVirtualFiles = binaryRootsVirtualFiles,
        librarySources = null,
        isSdk = false,
    ) {
        private val librarySourceModuleImpl by lazy(LazyThreadSafetyMode.PUBLICATION) {
            librarySources as? KaLibrarySourceModuleImpl ?: error(
                "The library module created to back library sources should have a `${KaLibrarySourceModuleImpl::class.simpleName}`."
            )
        }

        // The dependencies of a library module and its library sources should be in sync. Since dependencies are only added later by the
        // test infrastructure to the library source module (which is exported as a `KtTestModule`), the easiest way to synchronize
        // dependencies is to back the library module's dependencies with the library source module's dependencies.
        override val directRegularDependencies: MutableList<KaModule> get() = librarySourceModuleImpl.directRegularDependencies
        override val directFriendDependencies: MutableList<KaModule> get() = librarySourceModuleImpl.directFriendDependencies
        override val directDependsOnDependencies: MutableList<KaModule> get() = librarySourceModuleImpl.directDependsOnDependencies
    }

    val decompiledPsiFilesFromSourceJar = librarySourcesJars.flatMap { LibraryUtils.getAllPsiFilesFromJar(it, project) }
    val librarySourceKtModule = KaLibrarySourceModuleImpl(
        testModule.name,
        targetPlatform,
        GlobalSearchScope.filesScope(project, decompiledPsiFilesFromSourceJar.map { it.virtualFile }),
        project,
        binaryLibrary = libraryKtModule,
    )

    if (testModule.hasFallbackDependencies) {
        librarySourceKtModule.directRegularDependencies += KaLibraryFallbackDependenciesModuleImpl(libraryKtModule)
    }

    libraryKtModule.librarySources = librarySourceKtModule
    return KtTestModule(TestModuleKind.LibrarySource, testModule, librarySourceKtModule, decompiledPsiFilesFromSourceJar)
}
