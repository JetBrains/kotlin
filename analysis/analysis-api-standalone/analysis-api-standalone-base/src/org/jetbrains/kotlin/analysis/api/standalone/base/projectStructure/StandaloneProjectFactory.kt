/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.CorePackageIndex
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.*
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.impl.smartPointers.PsiClassReferenceTypePointerFactory
import com.intellij.psi.impl.smartPointers.SmartPointerManagerImpl
import com.intellij.psi.impl.smartPointers.SmartTypePointerManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.messages.ListenerDescriptor
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAccessibilityChecker
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModuleAnnotationsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneIndexCache
import org.jetbrains.kotlin.analysis.api.standalone.base.java.KotlinStandaloneJavaModuleAccessibilityChecker
import org.jetbrains.kotlin.analysis.api.standalone.base.java.KotlinStandaloneJavaModuleAnnotationsProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory.findJvmRootsForJavaFiles
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory.registerJavaPsiFacade
import org.jetbrains.kotlin.analysis.api.symbols.AdditionalKDocResolutionProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProviderCliImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.DummyFileAttributeService
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesDynamicCompoundIndex
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.picocontainer.PicoContainer
import java.nio.file.Path

object StandaloneProjectFactory {
    fun createProjectEnvironment(
        projectDisposable: Disposable,
        applicationEnvironmentMode: KotlinCoreApplicationEnvironmentMode,
        compilerConfiguration: CompilerConfiguration = CompilerConfiguration(),
    ): KotlinCoreProjectEnvironment {
        val applicationEnvironment = KotlinCoreEnvironment.getOrCreateApplicationEnvironment(
            projectDisposable = projectDisposable,
            compilerConfiguration,
            applicationEnvironmentMode,
        )

        registerApplicationExtensionPoints(applicationEnvironment)

        registerApplicationServices(applicationEnvironment)

        return object : KotlinCoreProjectEnvironment(projectDisposable, applicationEnvironment) {
            init {
                registerProjectServices(project)
                registerJavaPsiFacade(project)
            }

            override fun createProject(parent: PicoContainer, parentDisposable: Disposable): MockProject {
                return object : MockProject(parent, parentDisposable) {
                    @Suppress("UnstableApiUsage")
                    override fun createListener(descriptor: ListenerDescriptor): Any {
                        val listenerClass = loadClass<Any>(descriptor.listenerClassName, descriptor.pluginDescriptor)
                        val listener = listenerClass.getDeclaredConstructor(Project::class.java).newInstance(this)
                        return listener
                    }
                }
            }
        }
    }

    private fun registerApplicationServices(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        val application = applicationEnvironment.application
        if (application.getServiceIfCreated(KotlinStandaloneIndexCache::class.java) != null) {
            // application services already registered by som other threads, tests
            return
        }
        KotlinCoreEnvironment.underApplicationLock {
            if (application.getServiceIfCreated(KotlinStandaloneIndexCache::class.java) != null) {
                // application services already registered by som other threads, tests
                return
            }
            application.apply {
                registerService(KotlinStandaloneIndexCache::class.java, KotlinStandaloneIndexCache::class.java)
                registerService(ClsKotlinBinaryClassCache::class.java)
                registerService(
                    BuiltinsVirtualFileProvider::class.java,
                    BuiltinsVirtualFileProviderCliImpl()
                )
                registerService(FileAttributeService::class.java, DummyFileAttributeService::class.java)
            }
        }
    }

    @OptIn(KaExperimentalApi::class)
    private fun registerProjectServices(project: MockProject) {
        // TODO: rewrite KtResolveExtensionProviderForTest to avoid KtResolveExtensionProvider access before initialized project
        @Suppress("UnstableApiUsage")
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            KaResolveExtensionProvider.EP_NAME.name,
            KaResolveExtensionProvider::class.java
        )
    }

    private fun registerApplicationExtensionPoints(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        val applicationArea = applicationEnvironment.application.extensionArea

        if (!applicationArea.hasExtensionPoint(AdditionalKDocResolutionProvider.EP_NAME)) {
            KotlinCoreEnvironment.underApplicationLock {
                if (applicationArea.hasExtensionPoint(AdditionalKDocResolutionProvider.EP_NAME)) return@underApplicationLock
                CoreApplicationEnvironment.registerApplicationExtensionPoint(
                    AdditionalKDocResolutionProvider.EP_NAME,
                    AdditionalKDocResolutionProvider::class.java
                )
            }
        }

        if (!applicationArea.hasExtensionPoint(ClassTypePointerFactory.EP_NAME)) {
            KotlinCoreEnvironment.underApplicationLock {
                if (applicationArea.hasExtensionPoint(ClassTypePointerFactory.EP_NAME)) return@underApplicationLock
                CoreApplicationEnvironment.registerApplicationExtensionPoint(
                    ClassTypePointerFactory.EP_NAME,
                    ClassTypePointerFactory::class.java
                )
                applicationArea.getExtensionPoint(ClassTypePointerFactory.EP_NAME)
                    .registerExtension(PsiClassReferenceTypePointerFactory(), applicationEnvironment.application)
            }
        }
    }

    private fun registerJavaPsiFacade(project: MockProject) {
        with(project) {
            registerService(
                CoreJavaFileManager::class.java,
                this.getService(JavaFileManager::class.java) as CoreJavaFileManager
            )

            registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManager())
            registerService(InferredAnnotationsManager::class.java, MockInferredAnnotationsManager())

            // The Java language level must be configured before Java source files are parsed. See `findJvmRootsForJavaFiles`.
            setupHighestLanguageLevel()
        }
    }

    fun registerServicesForProjectEnvironment(
        environment: KotlinCoreProjectEnvironment,
        projectStructureProvider: KotlinStaticProjectStructureProvider,
        languageVersionSettings: LanguageVersionSettings = latestLanguageVersionSettings,
        jdkHome: Path? = null,
    ) {
        val project = environment.project

        KotlinCoreEnvironment.registerProjectExtensionPoints(project.extensionArea)

        project.registerService(SmartTypePointerManager::class.java, SmartTypePointerManagerImpl::class.java)
        project.registerService(SmartPointerManager::class.java, SmartPointerManagerImpl::class.java)

        val modules = projectStructureProvider.allModules
        project.registerService(KotlinProjectStructureProvider::class.java, projectStructureProvider)
        project.registerService(KotlinModuleDependentsProvider::class.java, KtStaticModuleDependentsProvider(modules))

        initialiseVirtualFileFinderServices(
            environment,
            modules,
            projectStructureProvider.allSourceFiles,
            languageVersionSettings,
            jdkHome,
        )
    }

    @OptIn(KaImplementationDetail::class)
    private fun initialiseVirtualFileFinderServices(
        environment: KotlinCoreProjectEnvironment,
        modules: List<KaModule>,
        sourceFiles: List<PsiFileSystemItem>,
        languageVersionSettings: LanguageVersionSettings,
        jdkHome: Path?,
    ) {
        val project = environment.project
        val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
        val javaModuleFinder = CliJavaModuleFinder(jdkHome?.toFile(), null, javaFileManager, project, null)
        val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

        val allSourceFileRoots = sourceFiles.map { JavaRoot(it.virtualFile, JavaRoot.RootType.SOURCE) }
        val jdkRoots = getDefaultJdkModuleRoots(javaModuleFinder, javaModuleGraph)

        // To implement the platform components required by the Analysis API for its own implementation of `JavaModuleResolver`, we can use
        // the compiler's `CliJavaModuleResolver`. This is a bit of an indirection (CLI java module resolver -> platform components ->
        // Analysis API java module resolver), but it's preferable over exposing `JavaModuleResolver` to all platforms. Furthermore, we can
        // still benefit from the common Analysis API implementation, for example its caching for Java module annotations.
        //
        // Note that the registered `JavaModuleResolver` will still be the `KaBaseJavaModuleResolver` registered by the Analysis API engine,
        // not `CliJavaModuleResolver`.
        val delegateJavaModuleResolver = CliJavaModuleResolver(
            javaModuleGraph,
            emptyList(),
            javaModuleFinder.systemModules.toList(),
            project,
        )
        project.registerService(
            KotlinJavaModuleAccessibilityChecker::class.java,
            KotlinStandaloneJavaModuleAccessibilityChecker(delegateJavaModuleResolver),
        )
        project.registerService(
            KotlinJavaModuleAnnotationsProvider::class.java,
            KotlinStandaloneJavaModuleAnnotationsProvider(delegateJavaModuleResolver),
        )

        val libraryRoots = getAllBinaryRoots(modules, environment.environment)

        val rootsWithSingleJavaFileRoots = buildList {
            addAll(libraryRoots)
            addAll(allSourceFileRoots)
            addAll(jdkRoots)
        }

        val (roots, singleJavaFileRoots) =
            rootsWithSingleJavaFileRoots.partition { (file) -> file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION }

        val corePackageIndex = project.getService(PackageIndex::class.java) as CorePackageIndex

        /**
         * We set `shouldOnlyFindFirstClass` to `false` for the following reason: In Standalone, we have a global view on the project, and
         * thus the index may contain multiple relevant classes with the same name which later (after the index access) need to be filtered
         * with a scope. See [org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndex.findClasses] for a more detailed explanation.
         */
        val rootsIndex = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = false).apply {
            addIndex(JvmDependenciesIndexImpl(roots, shouldOnlyFindFirstClass = false))
            indexedRoots.forEach { javaRoot ->
                if (javaRoot.file.isDirectory) {
                    if (javaRoot.type == JavaRoot.RootType.SOURCE) {
                        // NB: [JavaCoreProjectEnvironment#addSourcesToClasspath] calls:
                        //   1) [CoreJavaFileManager#addToClasspath], which is used to look up Java roots;
                        //   2) [CorePackageIndex#addToClasspath], which populates [PackageIndex]; and
                        //   3) [FileIndexFacade#addLibraryRoot], which conflicts with this SOURCE root when generating a library scope.
                        // Thus, here we manually call first two, which are used to:
                        //   1) create [PsiPackage] as a package resolution result; and
                        //   2) find directories by package name.
                        // With both supports, annotations defined in package-info.java can be properly propagated.
                        javaFileManager.addToClasspath(javaRoot.file)
                        corePackageIndex.addToClasspath(javaRoot.file)
                    } else {
                        environment.addSourcesToClasspath(javaRoot.file)
                    }
                }
            }
        }

        javaFileManager.initialize(
            rootsIndex,
            listOf(
                createPackagePartsProvider(libraryRoots + jdkRoots, languageVersionSettings)
                    .invoke(ProjectScope.getLibrariesScope(project))
            ),
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            usePsiClassFilesReading = true,
            perfManager = null, // Don't care about pure compiler performance in Analysis API
        )

        // Don't care about pure compiler performance in Analysis API
        val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, false, perfManager = null)
        project.registerService(VirtualFileFinderFactory::class.java, fileFinderFactory)
        project.registerService(MetadataFinderFactory::class.java, CliMetadataFinderFactory(fileFinderFactory))
    }

    fun getDefaultJdkModulePaths(
        project: Project,
        jdkHome: Path?,
    ): List<Path> {
        val javaFileManager = project.getService(JavaFileManager::class.java) as KotlinCliJavaFileManagerImpl
        val javaModuleFinder = CliJavaModuleFinder(jdkHome?.toFile(), null, javaFileManager, project, null)
        val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

        val javaRoots = getDefaultJdkModuleRoots(javaModuleFinder, javaModuleGraph)
        @OptIn(KaImplementationDetail::class)
        return javaRoots.map { LibraryUtils.getLibraryPathsForVirtualFiles(listOf(it.file)).single() }
    }

    /**
     * Computes the [JavaRoot]s of the JDK's default modules.
     *
     * @see ClasspathRootsResolver.addModularRoots
     */
    private fun getDefaultJdkModuleRoots(javaModuleFinder: CliJavaModuleFinder, javaModuleGraph: JavaModuleGraph): List<JavaRoot> {
        // In contrast to `ClasspathRootsResolver.addModularRoots`, we do not need to handle automatic Java modules because JDK modules
        // aren't automatic.
        return javaModuleGraph.getAllDependencies(javaModuleFinder.computeDefaultRootModules()).flatMap { moduleName ->
            val module = javaModuleFinder.findModule(moduleName) ?: return@flatMap emptyList<JavaRoot>()
            val result = module.getJavaModuleRoots()
            result
        }
    }

    /**
     * Note that [findJvmRootsForJavaFiles] parses the given [files] because it needs access to each file's package name. To avoid parsing
     * errors, [registerJavaPsiFacade] ensures that the Java language level is configured before [findJvmRootsForJavaFiles] is called.
     */
    fun findJvmRootsForJavaFiles(files: List<PsiJavaFile>): List<PsiDirectory> {
        if (files.isEmpty()) return emptyList()
        val result = mutableSetOf<PsiDirectory>()
        for (file in files) {
            val packageParts = file.packageName.takeIf { it.isNotEmpty() }?.split('.') ?: emptyList()
            var javaDir: PsiDirectory? = file.parent
            for (part in packageParts.reversed()) {
                if (javaDir?.name == part) {
                    javaDir = javaDir.parent
                } else {
                    // Error(ish): file package does not match file path.
                    // This could happen if, e.g., src/my/pkg/MyTest.java has package `test.pkg`.
                    // It is just best practice, not enforced by language spec.
                    // So, here, we just stop iterating upward the folder structure.
                    break
                }
            }
            javaDir?.let { result += it }
        }
        return result.toList()
    }

    fun getAllBinaryRoots(modules: List<KaModule>, environment: CoreApplicationEnvironment): List<JavaRoot> {
        return buildList {
            for (module in withAllTransitiveDependencies(modules)) {
                val roots = when (module) {
                    is KaLibraryModule -> module.getJavaRoots()
                    is KaLibrarySourceModule -> module.binaryLibrary.getJavaRoots()
                    else -> emptyList()
                }

                addAll(roots)
            }
        }
    }

    fun createLibraryModuleSearchScope(
        binaryVirtualFilesAndRoots: Collection<VirtualFile>,
        project: Project,
    ): GlobalSearchScope {
        return if (binaryVirtualFilesAndRoots.any { it.toNioPathOrNull() == null }) {
            // I.e., in-memory file system
            // Fall back: file-based search scope
            @Suppress("DEPRECATION")
            createSearchScopeByLibraryRoots(
                binaryVirtualFilesAndRoots,
                project,
            )
        } else {
            // Optimization: Trie-based search scope
            @Suppress("DEPRECATION")
            createTrieBasedSearchScopeByLibraryRoots(
                binaryVirtualFilesAndRoots,
                project,
            )
        }
    }

    fun createLibraryModuleSearchScope(
        binaryRoots: Collection<Path>,
        binaryVirtualFiles: Collection<VirtualFile>,
        environment: CoreApplicationEnvironment,
        project: Project,
    ): GlobalSearchScope {
        return if (binaryVirtualFiles.any { it.toNioPathOrNull() == null }) {
            // I.e., in-memory file system
            // Fall back: file-based search scope
            @Suppress("DEPRECATION")
            createSearchScopeByLibraryRoots(
                binaryRoots,
                binaryVirtualFiles,
                environment,
                project,
            )
        } else {
            // Optimization: Trie-based search scope
            @Suppress("DEPRECATION")
            createTrieBasedSearchScopeByLibraryRoots(
                binaryRoots,
                binaryVirtualFiles,
                environment,
                project,
            )
        }
    }

    @Deprecated(
        "This function will become private. Use `createLibraryModuleSearchScope` instead.",
        replaceWith = ReplaceWith("createLibraryModuleSearchScope(binaryRoots, binaryVirtualFiles, environment, project)"),
    )
    fun createSearchScopeByLibraryRoots(
        binaryRoots: Collection<Path>,
        binaryVirtualFiles: Collection<VirtualFile>,
        environment: CoreApplicationEnvironment,
        project: Project,
    ): GlobalSearchScope {
        val binaryVirtualFilesAndRoots = getVirtualFilesForLibraryRoots(binaryRoots, environment) + binaryVirtualFiles
        return createSearchScopeByLibraryRoots(binaryVirtualFilesAndRoots, project)
    }

    private fun createSearchScopeByLibraryRoots(
        binaryVirtualFilesAndRoots: Collection<VirtualFile>,
        project: Project,
    ): GlobalSearchScope {
        @OptIn(KaImplementationDetail::class)
        val virtualFileUrls = buildSet {
            for (root in binaryVirtualFilesAndRoots) {
                LibraryUtils.getAllVirtualFilesFromRoot(root, includeRoot = true)
                    .mapTo(this) { it.url }
            }
        }

        return object : GlobalSearchScope(project) {
            override fun contains(file: VirtualFile): Boolean = file.url in virtualFileUrls

            override fun isSearchInModuleContent(aModule: Module): Boolean = false

            override fun isSearchInLibraries(): Boolean = true

            override fun toString(): String = virtualFileUrls.toString()
        }
    }

    @Deprecated(
        "This function will become private. Use `createLibraryModuleSearchScope` instead.",
        replaceWith = ReplaceWith("createLibraryModuleSearchScope(binaryRoots, binaryVirtualFiles, environment, project)"),
    )
    fun createTrieBasedSearchScopeByLibraryRoots(
        binaryRoots: Collection<Path>,
        binaryVirtualFiles: Collection<VirtualFile>,
        environment: CoreApplicationEnvironment,
        project: Project,
    ): GlobalSearchScope {
        val virtualFiles = getVirtualFilesForLibraryRoots(binaryRoots, environment) + binaryVirtualFiles
        return createTrieBasedSearchScopeByLibraryRoots(virtualFiles, project)
    }

    private fun createTrieBasedSearchScopeByLibraryRoots(
        binaryVirtualFilesAndRoots: Collection<VirtualFile>,
        project: Project,
    ): GlobalSearchScope {
        return LibraryRootsSearchScope(binaryVirtualFilesAndRoots, project)
    }

    private class SimpleTrie(paths: List<String>) {
        class TrieNode {
            var isTerminal: Boolean = false
        }

        val root = TrieNode()

        private val m = mutableMapOf<Pair<TrieNode, String>, TrieNode>().apply {
            paths.forEach { path ->
                var p = root
                for (d in path.trim('/').split('/')) {
                    p = getOrPut(Pair(p, d)) { TrieNode() }
                }
                p.isTerminal = true
            }
        }

        fun contains(s: String): Boolean {
            var p = root
            for (d in s.trim('/').split('/')) {
                p = m.get(Pair(p, d))?.also {
                    if (it.isTerminal)
                        return true
                } ?: return false
            }
            return false
        }
    }

    private class LibraryRootsSearchScope(
        roots: Collection<VirtualFile>,
        project: Project,
    ) : GlobalSearchScope(project) {
        val trie: SimpleTrie = SimpleTrie(roots.map { it.path })

        override fun contains(file: VirtualFile): Boolean {
            return trie.contains(file.path)
        }

        override fun isSearchInModuleContent(aModule: Module): Boolean = false

        override fun isSearchInLibraries(): Boolean = true
    }

    fun getVirtualFilesForLibraryRoots(
        roots: Collection<Path>,
        environment: CoreApplicationEnvironment,
    ): List<VirtualFile> {
        @OptIn(KaImplementationDetail::class)
        return LibraryUtils.getVirtualFilesForLibraryRoots(roots, environment)
    }

    private fun withAllTransitiveDependencies(ktModules: List<KaModule>): List<KaModule> {
        val visited = hashSetOf<KaModule>()
        val stack = ktModules.toMutableList()
        while (stack.isNotEmpty()) {
            val module = stack.popLast()
            if (module in visited) continue
            visited += module
            for (dependency in module.allDependencies()) {
                if (dependency !in visited) {
                    stack += dependency
                }
            }
        }
        return visited.toList()
    }

    private fun KaModule.allDependencies(): List<KaModule> = buildList {
        addAll(allDirectDependencies())
        when (this) {
            is KaLibrarySourceModule -> {
                add(binaryLibrary)
            }
            is KaLibraryModule -> {
                addIfNotNull(librarySources)
            }
        }
    }

    private fun KaLibraryModule.getJavaRoots(): List<JavaRoot> {
        return binaryVirtualFiles.map { root ->
            JavaRoot(root, JavaRoot.RootType.BINARY)
        }
    }

    fun createPackagePartsProvider(
        libraryRoots: List<JavaRoot>,
        languageVersionSettings: LanguageVersionSettings = latestLanguageVersionSettings,
    ): (GlobalSearchScope) -> JvmPackagePartProvider = { scope ->
        JvmPackagePartProvider(languageVersionSettings, scope).apply {
            addRoots(libraryRoots, MessageCollector.NONE)
        }
    }

    private val latestLanguageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)
}