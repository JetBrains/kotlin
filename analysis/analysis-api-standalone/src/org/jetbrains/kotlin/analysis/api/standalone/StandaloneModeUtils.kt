/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.decompiled.light.classes.fe10.KotlinDeclarationInCompiledFileSearcherFE10Impl
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.decompiler.stub.file.CachedAttributeData
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.impl.ProjectStructureProviderByCompilerConfiguration
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl
import org.jetbrains.kotlin.idea.references.KotlinFirReferenceContributor
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.light.classes.symbol.IDEKotlinAsJavaFirSupport
import org.jetbrains.kotlin.light.classes.symbol.caches.SymbolLightClassFacadeCache
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtFile
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Configure Application environment for Analysis API standalone mode.
 *
 * In particular, this will register:
 *   * [KotlinReferenceProvidersService]
 *   * [KotlinReferenceProviderContributor]
 *   * [ClsKotlinBinaryClassCache]
 *   * [FileAttributeService]
 *   * [FileTypeFileViewProviders]
 *   * [KotlinClassFileDecompiler]
 *   * [KotlinBuiltInDecompiler]
 */
public fun configureApplicationEnvironment(app: MockApplication) {
    if (app.getServiceIfCreated(KotlinReferenceProvidersService::class.java) == null) {
        app.registerService(
            KotlinReferenceProvidersService::class.java,
            HLApiReferenceProviderService::class.java
        )
    }
    if (app.getServiceIfCreated(KotlinReferenceProviderContributor::class.java) == null) {
        app.registerService(
            KotlinReferenceProviderContributor::class.java,
            KotlinFirReferenceContributor::class.java
        )
    }

    if (app.getServiceIfCreated(ClsKotlinBinaryClassCache::class.java) == null) {
        app.registerService(ClsKotlinBinaryClassCache::class.java)
        app.registerService(FileAttributeService::class.java, DummyFileAttributeService)

        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(
            JavaClassFileType.INSTANCE,
            ClassFileViewProviderFactory()
        )

        @Suppress("DEPRECATION")
        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST)
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST)
        }

        app.registerService(
            KotlinDeclarationInCompiledFileSearcher::class.java,
            KotlinDeclarationInCompiledFileSearcherFE10Impl::class.java
        )
    }
}

private object DummyFileAttributeService : FileAttributeService {
    override fun <T> write(file: VirtualFile, id: String, value: T, writeValueFun: (DataOutput, T) -> Unit): CachedAttributeData<T> {
        return CachedAttributeData(value, 0)
    }

    override fun <T> read(file: VirtualFile, id: String, readValueFun: (DataInput) -> T): CachedAttributeData<T>? {
        return null
    }
}

/**
 * Configure Project environment for Analysis API standalone mode.
 *
 * In particular, this will register:
 *   * [KtAnalysisSessionProvider]
 *   * [KotlinAsJavaSupport] (a FIR version)
 *   * [SymbolLightClassFacadeCache] for FIR light class support
 *   * [ClsJavaStubByVirtualFileCache]
 *   * [KotlinModificationTrackerFactory]
 *   * [LLFirResolveStateService]
 *   * [FirSealedClassInheritorsProcessorFactory]
 *   * [KtModuleScopeProvider]
 *   * [ProjectStructureProvider]
 *   * [KotlinDeclarationProviderFactory]
 *   * [KotlinPackageProviderFactory]
 *   * [PackagePartProviderFactory]
 *
 *  Note that [ProjectStructureProvider] is built by using
 *    * given [ktFiles] as Kotlin sources
 *    * other Java sources in [compilerConfig] (set via [addJavaSourceRoots])
 *    * JVM class paths in [compilerConfig] (set via [addJvmClasspathRoots]) as library.
 *
 *  To make sure the same instance of [CoreJarFileSystem] is used (and thus file lookup in jars is cached),
 *    pass [jarFileSystem] from [KotlinCoreEnvironment] if available.
 */
public fun configureProjectEnvironment(
    project: MockProject,
    compilerConfig: CompilerConfiguration,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
) {
    val ktFiles = getKtFilesFromPaths(project, getSourceFilePaths(compilerConfig))
    configureProjectEnvironment(project, compilerConfig, ktFiles, packagePartProvider, jarFileSystem)
}

private fun getSourceFilePaths(
    compilerConfig: CompilerConfiguration,
): Set<String> {
    return buildSet {
        compilerConfig.javaSourceRoots.forEach { srcRoot ->
            val path = Paths.get(srcRoot)
            if (Files.isDirectory(path)) {
                // E.g., project/app/src
                Files.walk(path)
                    .filter(Files::isRegularFile)
                    .forEach { add(it.toString()) }
            } else {
                // E.g., project/app/src/some/pkg/main.kt
                add(srcRoot)
            }
        }
    }
}

private fun getKtFilesFromPaths(
    project: Project,
    paths: Collection<String>,
): List<KtFile> {
    val fs = StandardFileSystems.local()
    val psiManager = PsiManager.getInstance(project)
    return buildList {
        for (path in paths) {
            val vFile = fs.findFileByPath(path) ?: continue
            val ktFile = psiManager.findFile(vFile) as? KtFile ?: continue
            add(ktFile)
        }
    }
}

internal fun configureProjectEnvironment(
    project: MockProject,
    compilerConfig: CompilerConfiguration,
    sourceKtFiles: List<KtFile>,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
) {
    reRegisterJavaElementFinder(project)

    // FIR LC
    project.registerService(
        SymbolLightClassFacadeCache::class.java
    )
    project.registerService(
        ClsJavaStubByVirtualFileCache::class.java
    )

    project.picoContainer.registerComponentInstance(
        KotlinModificationTrackerFactory::class.qualifiedName,
        KotlinStaticModificationTrackerFactory()
    )
    RegisterComponentService.registerLLFirResolveStateService(project)
    project.picoContainer.registerComponentInstance(
        FirSealedClassInheritorsProcessorFactory::class.qualifiedName,
        object : FirSealedClassInheritorsProcessorFactory() {
            override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
                return SealedClassInheritorsProviderImpl
            }
        }
    )
    project.picoContainer.registerComponentInstance(
        KtModuleScopeProvider::class.qualifiedName,
        KtModuleScopeProviderImpl()
    )

    val projectStructureProvider =
        ProjectStructureProviderByCompilerConfiguration(
            compilerConfig,
            project,
            sourceKtFiles,
            jarFileSystem
        )
    project.picoContainer.registerComponentInstance(
        ProjectStructureProvider::class.qualifiedName,
        projectStructureProvider
    )
    project.picoContainer.registerComponentInstance(
        KotlinDeclarationProviderFactory::class.qualifiedName,
        KotlinStaticDeclarationProviderFactory(
            project,
            sourceKtFiles,
            projectStructureProvider.libraryModules,
            jarFileSystem
        )
    )
    project.picoContainer.registerComponentInstance(
        KotlinPackageProviderFactory::class.qualifiedName,
        KotlinStaticPackageProviderFactory(sourceKtFiles)
    )
    project.picoContainer.registerComponentInstance(
        PackagePartProviderFactory::class.qualifiedName,
        object : PackagePartProviderFactory() {
            override fun createPackagePartProviderForLibrary(scope: GlobalSearchScope): PackagePartProvider {
                return packagePartProvider(scope)
            }
        }
    )
}

@OptIn(InvalidWayOfUsingAnalysisSession::class)
private fun reRegisterJavaElementFinder(project: Project) {
    PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)
    with(project as MockProject) {
        picoContainer.registerComponentInstance(
            KtAnalysisSessionProvider::class.qualifiedName,
            KtFirAnalysisSessionProvider(this)
        )
        picoContainer.unregisterComponent(KotlinAsJavaSupport::class.qualifiedName)
        picoContainer.registerComponentInstance(
            KotlinAsJavaSupport::class.qualifiedName,
            IDEKotlinAsJavaFirSupport(project)
        )
    }
    @Suppress("DEPRECATION")
    PsiElementFinder.EP.getPoint(project).registerExtension(JavaElementFinder(project))
}
