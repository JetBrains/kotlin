/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.TestModuleStructureFactory.addLibraryDependencies
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.TestModuleStructureFactory.getScopeForLibraryByRoots
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

private typealias ModulesByName = Map<String, KtTestModule>

object TestModuleStructureFactory {
    fun createProjectStructureByTestStructure(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure {
        val modules = createModules(moduleStructure, testServices, project)

        val modulesByName = modules.associateBy { it.testModule.name }

        val libraryCache = LibraryCache(testServices, project)

        for (ktTestModule in modules) {
            libraryCache.registerLibraryModuleIfNeeded(ktTestModule.ktModule)
            ktTestModule.addDependencies(testServices, modulesByName, libraryCache)
        }

        return KtTestModuleStructure(moduleStructure, modules, libraryCache.libraryModules)
    }

    /**
     * The test infrastructure ensures that the given [moduleStructure] contains properly ordered dependencies: a [TestModule] can only
     * depend on test modules which precede it. Hence, this function does not need to order dependencies itself.
     *
     * @return A list of [KtTestModule]s in the same order as [TestModuleStructure.modules].
     */
    private fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): List<KtTestModule> {
        val moduleCount = moduleStructure.modules.size
        val existingModules = HashMap<String, KtTestModule>(moduleCount)
        val result = ArrayList<KtTestModule>(moduleCount)

        for (testModule in moduleStructure.modules) {
            val contextModuleName = testModule.directives.singleOrZeroValue(AnalysisApiTestDirectives.CONTEXT_MODULE)
            val contextModule = contextModuleName?.let(existingModules::getValue)

            val analysisContextModuleName = testModule.directives.singleOrZeroValue(AnalysisApiTestDirectives.ANALYSIS_CONTEXT_MODULE)
            val analysisContextModule = analysisContextModuleName?.let(existingModules::getValue)

            val dependencyBinaryRoots = testModule.regularDependencies.flatMap { dependency ->
                val libraryModule = existingModules.getValue(dependency.dependencyModule.name).ktModule as? KaLibraryModule
                libraryModule?.binaryRoots.orEmpty()
            }

            val ktTestModule = testServices
                .getKtModuleFactoryForTestModule(testModule)
                .createModule(testModule, analysisContextModule ?: contextModule, dependencyBinaryRoots, testServices, project)

            existingModules[testModule.name] = ktTestModule
            result.add(ktTestModule)
        }

        return result
    }

    private fun KtTestModule.addDependencies(
        testServices: TestServices,
        modulesByName: ModulesByName,
        libraryCache: LibraryCache,
    ) = when (ktModule) {
        is KaNotUnderContentRootModule, is KaBuiltinsModule -> {
            // Not-under-content-root and builtin modules have no external dependencies on purpose
        }
        is KaDanglingFileModule -> {
            // Dangling file modules get dependencies from their context
        }
        is KtModuleWithModifiableDependencies -> {
            addModuleDependencies(testModule, modulesByName, ktModule)
            addLibraryDependencies(testModule, testServices, ktModule, libraryCache)
        }
        else -> error("Unexpected module type: " + ktModule.javaClass.name)
    }

    private fun addModuleDependencies(
        testModule: TestModule,
        modulesByName: ModulesByName,
        ktModule: KtModuleWithModifiableDependencies,
    ) {
        testModule.allDependencies.forEach { dependency ->
            val dependencyKtModule = modulesByName.getValue(dependency.dependencyModule.name).ktModule
            when (dependency.relation) {
                DependencyRelation.RegularDependency -> ktModule.directRegularDependencies.add(dependencyKtModule)
                DependencyRelation.FriendDependency -> ktModule.directFriendDependencies.add(dependencyKtModule)
                DependencyRelation.DependsOnDependency -> ktModule.directDependsOnDependencies.add(dependencyKtModule)
            }
        }
    }

    private fun addLibraryDependencies(
        testModule: TestModule,
        testServices: TestServices,
        ktModule: KtModuleWithModifiableDependencies,
        libraryCache: LibraryCache,
    ) {
        val project = ktModule.project

        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(testModule)

        val classpathRoots = compilerConfiguration[CLIConfigurationKeys.CONTENT_ROOTS, emptyList()]
            .mapNotNull { (it as? JvmClasspathRoot)?.file?.toPath() }

        if (classpathRoots.isNotEmpty()) {
            val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(testModule.directives)
            val jdkHome = JvmEnvironmentConfigurator.getJdkHome(jdkKind)?.toPath()
                ?: JvmEnvironmentConfigurator.getJdkClasspathRoot(jdkKind)?.toPath()
                ?: Paths.get(System.getProperty("java.home"))

            val (jdkRoots, libraryRoots) = classpathRoots.partition { jdkHome != null && it.startsWith(jdkHome) }

            val targetPlatform = testModule.targetPlatform(testServices)
            if (targetPlatform.isJvm() && jdkRoots.isNotEmpty()) {
                ktModule.directRegularDependencies.add(
                    libraryCache.getOrCreateJdkModule(jdkRoots)
                )
            }

            for (libraryRoot in libraryRoots) {
                check(libraryRoot.extension.let { it == "jar" || it == "klib" }) {
                    "Unknown library format: ${libraryRoot.absolute()}"
                }

                val platform = when (libraryRoot.extension.toLowerCaseAsciiOnly()) {
                    "jar" -> JvmPlatforms.defaultJvmPlatform
                    else -> targetPlatform
                }

                val libraryModule = libraryCache.getOrCreateLibraryModule(libraryRoot) {
                    createLibraryModule(project, libraryRoot, platform, testServices)
                }

                // Multiple library roots may correspond to the same library module, which should only be added once.
                if (libraryModule !in ktModule.directRegularDependencies) {
                    ktModule.directRegularDependencies.add(libraryModule)
                }
            }
        }

        val jsLibraryRootPaths = compilerConfiguration[JSConfigurationKeys.LIBRARIES].orEmpty()

        for (libraryRootPath in jsLibraryRootPaths) {
            val libraryRoot = Paths.get(libraryRootPath)
            check(libraryRoot.extension == KLIB_FILE_EXTENSION)

            val libraryModule = libraryCache.getOrCreateLibraryModule(libraryRoot) {
                createLibraryModule(project, libraryRoot, JsPlatforms.defaultJsPlatform, testServices)
            }

            ktModule.directRegularDependencies.add(libraryModule)
        }
    }

    private fun createLibraryModule(
        project: Project,
        libraryFile: Path,
        platform: TargetPlatform,
        testServices: TestServices,
    ): KaLibraryModuleImpl {
        check(libraryFile.exists()) { "Library $libraryFile does not exist" }

        val libraryName = libraryFile.nameWithoutExtension
        val libraryScope = getScopeForLibraryByRoots(project, listOf(libraryFile), testServices)
        return KaLibraryModuleImpl(libraryName, platform, libraryScope, project, listOf(libraryFile), librarySources = null, isSdk = false)
    }

    fun getScopeForLibraryByRoots(project: Project, roots: Collection<Path>, testServices: TestServices): GlobalSearchScope {
        return StandaloneProjectFactory.createSearchScopeByLibraryRoots(
            roots,
            emptyList(),
            testServices.environmentManager.getApplicationEnvironment(),
            project,
        )
    }

    fun createSourcePsiFiles(
        testModule: TestModule,
        testServices: TestServices,
        project: Project,
    ): List<PsiFile> {
        return testModule.files.map { testFile ->
            when {
                testFile.isKtFile -> {
                    val fileText = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                    KtTestUtil.createFile(testFile.name, fileText, project)
                }

                testFile.isJavaFile || testFile.isExternalAnnotation -> {
                    val filePath = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(testFile)
                    val virtualFile =
                        testServices.environmentManager.getApplicationEnvironment().localFileSystem.findFileByIoFile(filePath)
                            ?: error("Virtual file not found for $filePath")
                    PsiManager.getInstance(project).findFile(virtualFile)
                        ?: error("PsiFile file not found for $filePath")
                }

                else -> error("Unexpected file ${testFile.name}")
            }
        }
    }
}

private class LibraryCache(
    private val testServices: TestServices,
    private val project: Project,
) {
    /**
     * A mapping from a binary root [Path] to its [KaLibraryModule].
     *
     * [binaryRootToLibraryModule] maps single binary roots to their [KaLibraryModule] even for multi-root libraries. In that case, the
     * mapping contains multiple entries for the [KaLibraryModule], and each binary root can individually resolve to its multi-root
     * [KaLibraryModule]. This also means that each binary root must be associated with *exactly* one [KaLibraryModule] in tests.
     *
     * We need this functionality because [addLibraryDependencies][TestModuleStructureFactory.addLibraryDependencies] processes a flat list
     * of all binary root dependencies of the [TestModule], taken from its compiler configuration. From this flat list, we cannot associate
     * *sets of* binary roots with one another.
     */
    private val binaryRootToLibraryModule: MutableMap<Path, KaLibraryModule> = mutableMapOf()

    /**
     * Each test should have a single JDK configuration, so there should only be a need for a single JDK module.
     */
    private var jdkModule: KaLibraryModule? = null

    val libraryModules: List<KaLibraryModule>
        get() = listOfNotNull(jdkModule) + binaryRootToLibraryModule.values.distinct()

    /**
     * Registers a [KaLibraryModule] in the [binaryRootToLibraryModule] mapping.
     *
     * A main module may be a binary library module, which may be a dependency of subsequent main modules. We need to add such a module to
     * the [binaryRootToLibraryModule] mapping before it is processed as a dependency. Otherwise, when another module's binary dependency is
     * processed, [addLibraryDependencies] will create a *duplicate* binary library module with the same roots and name as the already
     * existing binary library module.
     *
     * When the [KaLibraryModule] has multiple binary roots, [registerLibraryModuleIfNeeded] creates as many entries in the
     * [binaryRootToLibraryModule] mapping to fulfill the requirements outlined in the KDoc of [binaryRootToLibraryModule].
     */
    fun registerLibraryModuleIfNeeded(module: KaModule) {
        if (module !is KaLibraryModule) return

        module.binaryRoots.forEach { binaryRoot ->
            check(binaryRoot !in binaryRootToLibraryModule) {
                "Each binary root should be uniquely associated with a single library module.\n" +
                        "Binary root: $binaryRoot\n" +
                        "Library module: $module"
            }
            binaryRootToLibraryModule.put(binaryRoot, module)
        }
    }

    inline fun getOrCreateLibraryModule(binaryRoot: Path, createLibraryModule: () -> KaLibraryModule): KaLibraryModule {
        return binaryRootToLibraryModule.getOrPut(binaryRoot) { createLibraryModule() }
    }

    fun getOrCreateJdkModule(jdkRoots: List<Path>): KaLibraryModule {
        require(jdkRoots.isNotEmpty()) { "At least one JDK root is required." }

        jdkModule?.let { return it }

        val jdkScope = getScopeForLibraryByRoots(project, jdkRoots, testServices)
        val jdkModule = KaLibraryModuleImpl(
            "jdk",
            JvmPlatforms.defaultJvmPlatform,
            jdkScope,
            project,
            jdkRoots,
            librarySources = null,
            isSdk = true,
        )

        this.jdkModule = jdkModule
        return jdkModule
    }
}
