/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.CorePackageIndex
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil.JAR_PROTOCOL
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.analysis.project.structure.*
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
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.nio.file.Path

object StandaloneProjectFactory {
    fun createProjectEnvironment(
        projectDisposable: Disposable,
        applicationDisposable: Disposable,
        compilerConfiguration: CompilerConfiguration = CompilerConfiguration(),
    ): KotlinCoreProjectEnvironment {
        val applicationEnvironment =
            KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForTests(applicationDisposable, compilerConfiguration)

        return KotlinCoreProjectEnvironment(projectDisposable, applicationEnvironment).apply {
            registerJavaPsiFacade(project)
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
        projectStructureProvider: ProjectStructureProvider,
        modules: List<KtModule>,
        sourceFiles: List<PsiFileSystemItem>,
        languageVersionSettings: LanguageVersionSettings = latestLanguageVersionSettings,
        jdkHome: Path? = null,
    ) {
        val project = environment.project

        KotlinCoreEnvironment.registerProjectExtensionPoints(project.extensionArea)
        KotlinCoreEnvironment.registerProjectServices(project)

        project.registerService(ProjectStructureProvider::class.java, projectStructureProvider)
        initialiseVirtualFileFinderServices(environment, modules, sourceFiles, languageVersionSettings, jdkHome)
    }

    private fun initialiseVirtualFileFinderServices(
        environment: KotlinCoreProjectEnvironment,
        modules: List<KtModule>,
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
        val rootsIndex = JvmDependenciesDynamicCompoundIndex().apply {
            addIndex(JvmDependenciesIndexImpl(roots))
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
                createPackagePartsProvider(project, libraryRoots + jdkRoots, languageVersionSettings)
                    .invoke(ProjectScope.getLibrariesScope(project))
            ),
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            true
        )

        val finderFactory = CliVirtualFileFinderFactory(rootsIndex, false)

        project.registerService(MetadataFinderFactory::class.java, finderFactory)
        project.registerService(VirtualFileFinderFactory::class.java, finderFactory)
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

    fun getAllBinaryRoots(
        modules: List<KtModule>,
        environment: KotlinCoreProjectEnvironment
    ): List<JavaRoot> = withAllTransitiveDependencies(modules)
        .filterIsInstance<KtBinaryModule>()
        .flatMap { it.getJavaRoots(environment) }

    fun getVirtualFilesForLibraryRoots(
        roots: Collection<Path>,
        environment: KotlinCoreProjectEnvironment
    ): List<VirtualFile> {
        return roots.mapNotNull { path ->
            val pathString = path.toAbsolutePath().toString()
            when {
                pathString.endsWith(JAR_PROTOCOL) -> {
                    environment.environment.jarFileSystem.findFileByPath(pathString + JAR_SEPARATOR)
                }

                pathString.contains(JAR_SEPARATOR) -> {
                    environment.environment.jrtFileSystem?.findFileByPath(adjustModulePath(pathString))
                }

                else -> {
                    VirtualFileManager.getInstance().findFileByNioPath(path)
                }
            }
        }
    }

    private fun withAllTransitiveDependencies(ktModules: List<KtModule>): List<KtModule> {
        val visited = hashSetOf<KtModule>()
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

    private fun KtModule.allDependencies(): List<KtModule> = buildList {
        addAll(allDirectDependencies())
        when (this) {
            is KtLibrarySourceModule -> {
                add(binaryLibrary)
            }
            is KtLibraryModule -> {
                addIfNotNull(librarySources)
            }
        }
    }

    private fun KtBinaryModule.getJavaRoots(
        environment: KotlinCoreProjectEnvironment,
    ): List<JavaRoot> {
        return getVirtualFilesForLibraryRoots(getBinaryRoots(), environment).map { root -> JavaRoot(root, JavaRoot.RootType.BINARY)}
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
            // See [LLFirJavaFacadeForBinaries#getBinaryPath] for a similar hack.
            val (libHomePath, pathInImage) = CoreJrtFileSystem.splitPath(pathString)
            libHomePath + JAR_SEPARATOR + "modules/$pathInImage"
        } else
            pathString
    }

    fun createPackagePartsProvider(
        project: MockProject,
        libraryRoots: List<JavaRoot>,
        languageVersionSettings: LanguageVersionSettings = latestLanguageVersionSettings,
    ): (GlobalSearchScope) -> JvmPackagePartProvider = { scope ->
        JvmPackagePartProvider(languageVersionSettings, scope).apply {
            addRoots(libraryRoots, MessageCollector.NONE)
            (ModuleAnnotationsResolver
                .getInstance(project) as CliModuleAnnotationsResolver)
                .addPackagePartProvider(this)
        }
    }

    private val latestLanguageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)
}