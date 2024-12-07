/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.impl.smartPointers.PsiClassReferenceTypePointerFactory
import com.intellij.psi.impl.smartPointers.SmartPointerManagerImpl
import com.intellij.psi.impl.smartPointers.SmartTypePointerManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil.JAR_PROTOCOL
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import com.intellij.util.messages.ListenerDescriptor
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinFakeClsStubsCache
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
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.picocontainer.PicoContainer
import java.nio.file.Path
import java.nio.file.Paths

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
        if (application.getServiceIfCreated(KotlinFakeClsStubsCache::class.java) != null) {
            // application services already registered by som other threads, tests
            return
        }
        KotlinCoreEnvironment.underApplicationLock {
            if (application.getServiceIfCreated(KotlinFakeClsStubsCache::class.java) != null) {
                // application services already registered by som other threads, tests
                return
            }
            application.apply {
                registerService(KotlinFakeClsStubsCache::class.java, KotlinFakeClsStubsCache::class.java)
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

        project.registerService(
            JavaModuleResolver::class.java,
            CliJavaModuleResolver(javaModuleGraph, emptyList(), javaModuleFinder.systemModules.toList(), project)
        )

        val libraryRoots = getAllBinaryRoots(modules, environment)

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
            true
        )

        val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, false)
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
        return javaRoots.map { getBinaryPath(it.file) }
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

    fun getAllBinaryRoots(modules: List<KaModule>, environment: KotlinCoreProjectEnvironment): List<JavaRoot> {
        return buildList {
            for (module in withAllTransitiveDependencies(modules)) {
                val roots = when (module) {
                    is KaLibraryModule -> module.getJavaRoots(environment)
                    is KaLibrarySourceModule -> module.binaryLibrary.getJavaRoots(environment)
                    else -> emptyList()
                }

                addAll(roots)
            }
        }
    }

    fun createSearchScopeByLibraryRoots(
        binaryRoots: Collection<Path>,
        binaryVirtualFiles: Collection<VirtualFile>,
        environment: KotlinCoreProjectEnvironment,
    ): GlobalSearchScope {
        val virtualFileUrlsFromBinaryRoots = getVirtualFileUrlsForLibraryRootsRecursively(binaryRoots, environment)
        val virtualFileUrlsFromBinaryVirtualFiles = getVirtualFileUrlsForLibraryRootsRecursively(binaryVirtualFiles)
        val virtualFileUrls = virtualFileUrlsFromBinaryRoots + virtualFileUrlsFromBinaryVirtualFiles

        return object : GlobalSearchScope(environment.project) {
            override fun contains(file: VirtualFile): Boolean = file.url in virtualFileUrls

            override fun isSearchInModuleContent(aModule: Module): Boolean = false

            override fun isSearchInLibraries(): Boolean = true

            override fun toString(): String = virtualFileUrls.toString()
        }
    }

    @OptIn(KaImplementationDetail::class)
    private fun getVirtualFileUrlsForLibraryRootsRecursively(
        roots: Collection<Path>,
        environment: KotlinCoreProjectEnvironment,
    ): Set<String> =
        buildSet {
            for (root in getVirtualFilesForLibraryRoots(roots, environment)) {
                LibraryUtils.getAllVirtualFilesFromRoot(root, includeRoot = true)
                    .mapTo(this) { it.url }
            }
        }

    @OptIn(KaImplementationDetail::class)
    private fun getVirtualFileUrlsForLibraryRootsRecursively(
        binaryVirtualFiles: Collection<VirtualFile>
    ): Set<String> =
        buildSet {
            for (vf in binaryVirtualFiles) {
                LibraryUtils.getAllVirtualFilesFromRoot(vf, includeRoot = true)
                    .mapTo(this) { it.url }
            }
        }

    fun getVirtualFilesForLibraryRoots(
        roots: Collection<Path>,
        environment: KotlinCoreProjectEnvironment,
    ): List<VirtualFile> {
        return roots.mapNotNull { path ->
            val pathString = FileUtil.toSystemIndependentName(path.toAbsolutePath().toString())
            when {
                pathString.endsWith(JAR_PROTOCOL) || pathString.endsWith(KLIB_FILE_EXTENSION) -> {
                    environment.environment.jarFileSystem.findFileByPath(pathString + JAR_SEPARATOR)
                }

                pathString.contains(JAR_SEPARATOR) -> {
                    environment.environment.jrtFileSystem?.findFileByPath(adjustModulePath(pathString))
                }

                else -> {
                    VirtualFileManager.getInstance().findFileByNioPath(path)
                }
            }
        }.distinct()
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

    @OptIn(KaExperimentalApi::class)
    private fun KaLibraryModule.getJavaRoots(
        environment: KotlinCoreProjectEnvironment,
    ): List<JavaRoot> {
        val binaryRootsAsVirtualFiles = getVirtualFilesForLibraryRoots(binaryRoots, environment) + binaryVirtualFiles
        return binaryRootsAsVirtualFiles.map { root ->
            JavaRoot(root, JavaRoot.RootType.BINARY)
        }
    }

    private fun adjustModulePath(pathString: String): String {
        return if (pathString.contains(JAR_SEPARATOR)) {
            // URLs loaded from JDK point to module names in a JRT protocol format,
            // e.g., "jrt:///path/to/jdk/home!/java.base" (JRT protocol prefix + JDK home path + JAR separator + module name)
            // After protocol erasure, we will see "/path/to/jdk/home!/java.base" as a binary root.
            // CoreJrtFileSystem.CoreJrtHandler#findFile, which uses Path#resolve, finds a virtual file path to the file itself,
            // e.g., "/path/to/jdk/home!/modules/java.base". (JDK home path + JAR separator + actual file path)
            // To work with that JRT handler, a hacky workaround here is to add "modules" before the module name so that it can
            // find the actual file path.
            // See [LLFirJavaFacadeForBinaries#getBinaryPath] and [StandaloneProjectFactory#getBinaryPath] for a similar hack.
            val (libHomePath, pathInImage) = CoreJrtFileSystem.splitPath(pathString)
            libHomePath + JAR_SEPARATOR + "modules/$pathInImage"
        } else
            pathString
    }

    // From [LLFirJavaFacadeForBinaries#getBinaryPath]
    private fun getBinaryPath(virtualFile: VirtualFile): Path {
        val path = virtualFile.path
        return when {
            ".$JAR_PROTOCOL$JAR_SEPARATOR" in path ->
                Paths.get(path.substringBefore(JAR_SEPARATOR))
            JAR_SEPARATOR in path && "modules/" in path -> {
                // CoreJrtFileSystem.CoreJrtHandler#findFile, which uses Path#resolve, finds a virtual file path to the file itself,
                // e.g., "/path/to/jdk/home!/modules/java.base/java/lang/Object.class". (JDK home path + JAR separator + actual file path)
                // URLs loaded from JDK, though, point to module names in a JRT protocol format,
                // e.g., "jrt:///path/to/jdk/home!/java.base" (JRT protocol prefix + JDK home path + JAR separator + module name)
                // After splitting at the JAR separator, it is regarded as a root directory "/java.base".
                // To work with LibraryPathFilter, a hacky workaround here is to remove "modules/" from actual file path.
                // e.g. "/path/to/jdk/home!/java.base/java/lang/Object.class", which, from Path viewpoint, belongs to "/java.base",
                // after splitting at the JAR separator, in a similar way.
                // See [StandaloneProjectFactory#getAllBinaryRoots] for a similar hack.
                Paths.get(path.replace("modules/", ""))
            }
            else ->
                Paths.get(path)
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