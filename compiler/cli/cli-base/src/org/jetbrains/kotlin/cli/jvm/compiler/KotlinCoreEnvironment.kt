/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DEPRECATION_ERROR", "UnstableApiUsage")

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreApplicationEnvironment.registerExtensionPoint
import com.intellij.core.CoreJavaFileManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.jvm.facade.JvmElementProvider
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.impl.*
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.CliDiagnostics.INITIALIZATION_WARNING
import org.jetbrains.kotlin.cli.CliDiagnostics.ROOTS_RESOLUTION_WARNING
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.index.*
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaFixedElementSourceFactory
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.io.File

@OptIn(K1Deprecation::class, CoreEnvironmentDeprecation::class)
class KotlinCoreEnvironment private constructor(
    val projectEnvironment: ProjectEnvironment,
    val configuration: CompilerConfiguration
) {

    class ProjectEnvironment(
        disposable: Disposable,
        applicationEnvironment: KotlinCoreApplicationEnvironment,
        configuration: CompilerConfiguration
    ) : KotlinCoreProjectEnvironment(disposable, applicationEnvironment) {

        val jarFileSystem: VirtualFileSystem

        init {
            setIdeaIoUseFallback()

            val useFastJarFSFlag: Boolean? = configuration.get(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM)
            val useK2 =
                configuration.getBoolean(CommonConfigurationKeys.USE_FIR) || configuration.languageVersionSettings.languageVersion.usesK2

            when {
                useFastJarFSFlag == true && !useK2 -> {
                    configuration.report(
                        INITIALIZATION_WARNING,
                        "Using new faster version of JAR FS: it should make your build faster, " +
                                "but the new implementation is not thoroughly tested with language versions below 2.0"
                    )
                }
                useFastJarFSFlag == false && useK2 -> {
                    configuration.reportInfo("Using outdated version of JAR FS: it might make your build slower")
                }
            }

            // We enable FastJarFS by default since K2
            val useFastJarFS = useFastJarFSFlag ?: useK2

            jarFileSystem = when {
                configuration.getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING) -> {
                    applicationEnvironment.jarFileSystem
                }
                useFastJarFS -> {
                    val fastJarFs = applicationEnvironment.fastJarFileSystem
                    if (fastJarFs == null) {
                        configuration.report(
                            INITIALIZATION_WARNING,
                            "Your JDK doesn't seem to support mapped buffer unmapping, so the slower (old) version of JAR FS will be used"
                        )
                        applicationEnvironment.jarFileSystem
                    } else {
                        val outputJar = configuration.get(JVMConfigurationKeys.OUTPUT_JAR)
                        if (outputJar == null) {
                            fastJarFs
                        } else {
                            val contentRoots = configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
                            if (contentRoots?.any { it is JvmClasspathRoot && it.file.path == outputJar.path } == true) {
                                // See KT-61883
                                configuration.report(
                                    INITIALIZATION_WARNING,
                                    "JAR from the classpath ${outputJar.path} is reused as output JAR, so the slower (old) version of JAR FS will be used"
                                )
                                applicationEnvironment.jarFileSystem
                            } else {
                                fastJarFs
                            }
                        }
                    }
                }

                else -> applicationEnvironment.jarFileSystem
            }
        }

        private var extensionRegistered = false

        override fun preregisterServices() {
            registerProjectExtensionPoints(project.extensionArea)
        }

        fun registerExtensionsFromPlugins(configuration: CompilerConfiguration) {
            if (!extensionRegistered) {
                registerExtensionsFromPlugins(project, configuration)
                extensionRegistered = true
            }
        }

        override fun registerJavaPsiFacade() {
            with(project) {
                registerService(
                    CoreJavaFileManager::class.java,
                    this.getService(JavaFileManager::class.java) as CoreJavaFileManager
                )

                // We don't pass Disposable because in some tests, we manually unregister these extensions, and that leads to LOG.error
                // exception from `ExtensionPointImpl.doRegisterExtension`, because the registered extension can no longer be found
                // when the project is being disposed.
                // For example, see the `unregisterExtension` call in `GenerationUtils.compileFilesUsingFrontendIR`.
                // TODO: refactor this to avoid registering unneeded extensions in the first place, and avoid using deprecated API. (KT-64296)
                @Suppress("DEPRECATION")
                PsiElementFinder.EP.getPoint(project).registerExtension(PsiElementFinderImpl(this))

                registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManager())
                registerService(InferredAnnotationsManager::class.java, MockInferredAnnotationsManager())
            }

            super.registerJavaPsiFacade()
        }
    }

    private val classpathIndexes = JvmClasspathIndexes(configuration)

    val packagePartProviders: List<JvmPackagePartProvider>
        field = mutableListOf<JvmPackagePartProvider>()

    private val classpathRootsResolver: ClasspathRootsResolver
    private val initialRoots = ArrayList<JavaRoot>()

    init {
        projectEnvironment.configureProjectEnvironment(configuration)
        val project = projectEnvironment.project

        val javaFileManager = project.getService(CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

        val jdkHome = configuration.get(JVMConfigurationKeys.JDK_HOME)
        val releaseTarget = configuration.get(JVMConfigurationKeys.JDK_RELEASE)
        val javaModuleFinder = CliJavaModuleFinder(
            jdkHome,
            configuration,
            javaFileManager,
            releaseTarget
        )

        val outputDirectory =
            configuration.get(JVMConfigurationKeys.MODULES)?.singleOrNull()?.getOutputDirectory()
                ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)?.absolutePath

        val contentRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)

        classpathRootsResolver = ClasspathRootsResolver(
            configuration,
            configuration.getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES),
            this::contentRootToVirtualFile,
            javaModuleFinder,
            !configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE),
            outputDirectory?.let(this::findLocalFile),
            javaFileManager::findClassOnClasspath,
            releaseTarget,
            hasKotlinSources = contentRoots.any { it is KotlinSourceRoot },
        )

        (val initialRoots = roots, val javaModules = modules) = classpathRootsResolver.convertClasspathRoots(contentRoots)
        this.initialRoots.addAll(initialRoots)

        val [roots, singleJavaFileRoots] =
            initialRoots.partition { (file) -> file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION }

        // REPL updates classpath dynamically
        updateClasspathFromRootsIndex(classpathIndexes.addRoots(roots))

        val perfManager = configuration.perfManager

        javaFileManager.initialize(
            classpathIndexes.javaFileManagerIndex,
            packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            configuration.getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING),
            javaModuleFinder,
            perfManager,
        )

        val fileFinderFactory =
            CliVirtualFileFinderFactory(classpathIndexes.classFinderIndex, enableSearchInCtSym = releaseTarget != null, perfManager)
        project.registerService(
            JavaModuleResolver::class.java,
            CliJavaModuleResolver(
                classpathRootsResolver.javaModuleGraph, javaModules, javaModuleFinder.systemModules.toList(),
                fileFinderFactory.create(JvmClasspath.ProjectLibraries()),
            )
        )

        project.registerService(VirtualFileFinderFactory::class.java, fileFinderFactory)
        project.registerService(MetadataFinderFactory::class.java, CliMetadataFinderFactory(fileFinderFactory))

        project.putUserData(APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY, fun(roots: List<File>) {
            updateClasspath(roots.map { JavaSourceRoot(it, null) })
        })

        project.setupHighestLanguageLevel()
    }

    fun createPackagePartProvider(classpath: JvmClasspath): JvmPackagePartProvider {
        return JvmPackagePartProvider(configuration.languageVersionSettings, classpath).apply {
            addRoots(initialRoots, configuration)
            packagePartProviders += this
        }
    }

    private val applicationEnvironment: CoreApplicationEnvironment
        get() = projectEnvironment.environment

    val project: Project
        get() = projectEnvironment.project

    /**
     * The classpath roots this compilation indexed, in classpath order. A root is named by its identity
     * (`JvmClasspathRootId`) rather than by a location, so a classpath can be described without accessing a file
     * system; see `VfsBasedProjectEnvironment.registerIndexedClasspathRoots`.
     */
    val indexedClasspathRoots: List<VirtualFile>
        get() = classpathIndexes.javaFileManagerIndex.indexedRoots.map { it.file }.toList()

    private fun updateClasspathFromRootsIndex(index: JvmDependenciesIndex) {
        index.indexedRoots.forEach {
            projectEnvironment.addSourcesToClasspath(it.file)
        }
    }

    fun updateClasspath(contentRoots: List<ContentRoot>): List<File>? {
        // TODO: add new Java modules to CliJavaModuleResolver
        val newRoots = classpathRootsResolver.convertClasspathRoots(contentRoots).roots - initialRoots

        val unindexedRoots = classpathIndexes.javaFileManagerIndex.getUnindexedRoots(newRoots)
        if (unindexedRoots.isEmpty()) return null

        val newIndex = classpathIndexes.addRoots(unindexedRoots)
        updateClasspathFromRootsIndex(newIndex)

        if (packagePartProviders.isEmpty()) {
            initialRoots.addAll(newRoots)
        } else {
            for (packagePartProvider in packagePartProviders) {
                packagePartProvider.addRoots(newRoots, configuration)
            }
        }

        configuration.addAll(CLIConfigurationKeys.CONTENT_ROOTS, contentRoots - configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS))

        return newIndex.indexedRoots.map { (file) ->
            VfsUtilCore.virtualToIoFile(VfsUtilCore.getVirtualFileForJar(file) ?: file)
        }.toList()
    }

    private fun contentRootToVirtualFile(root: JvmContentRootBase): VirtualFile? =
        when (root) {
            is JvmClasspathRoot ->
                if (root.file.isFile) findJarRoot(root.file) else findExistingRoot(root, "Classpath entry")
            is JvmModulePathRoot ->
                if (root.file.isFile) findJarRoot(root.file) else findExistingRoot(root, "Java module root")
            is JavaSourceRoot ->
                findExistingRoot(root, "Java source root")
            is VirtualJvmClasspathRoot -> root.file
            else ->
                throw IllegalStateException("Unexpected root: $root")
        }

    fun findLocalFile(path: String): VirtualFile? =
        applicationEnvironment.localFileSystem.findFileByPath(path)

    private fun findExistingRoot(root: JvmContentRoot, rootDescription: String): VirtualFile? {
        return findLocalFile(root.file.absolutePath).also {
            if (it == null) {
                configuration.report(ROOTS_RESOLUTION_WARNING, "$rootDescription points to a non-existent location: ${root.file}")
            }
        }
    }

    private fun findJarRoot(file: File): VirtualFile? =
        projectEnvironment.jarFileSystem.findFileByPath("$file${URLUtil.JAR_SEPARATOR}")

    companion object {
        @JvmStatic
        @CoreEnvironmentDeprecation
        fun create(projectDisposable: Disposable, configuration: CompilerConfiguration): KotlinCoreEnvironment {
            setupIdeaStandaloneExecution()
            val appEnv = KotlinCoreApplicationEnvironment.getOrCreateApplicationEnvironment(projectDisposable)
            val projectEnv = ProjectEnvironment(projectDisposable, appEnv, configuration)
            val environment = KotlinCoreEnvironment(projectEnv, configuration)

            return environment
        }

        @JvmStatic
        @CoreEnvironmentDeprecation
        fun create(projectEnvironment: ProjectEnvironment, configuration: CompilerConfiguration): KotlinCoreEnvironment {
            return KotlinCoreEnvironment(projectEnvironment, configuration)
        }

        @JvmStatic
        @CoreEnvironmentDeprecation
        fun ProjectEnvironment.configureProjectEnvironment(configuration: CompilerConfiguration) {
            PersistentFSConstants::class.java.getDeclaredField("ourMaxIntellisenseFileSize")
                .apply { isAccessible = true }
                .setInt(null, FileUtilRt.LARGE_FOR_CONTENT_LOADING)

            registerExtensionsFromPlugins(configuration)
            registerProjectServices(project)

            for (extension in configuration.getCompilerExtensions(CompilerConfigurationExtension)) {
                extension.updateConfiguration(project, configuration)
            }
        }

        private fun registerExtensionsFromPlugins(project: MockProject, configuration: CompilerConfiguration) {
            fun createErrorMessage(extension: Any): String {
                return "The provided plugin ${extension.javaClass.name} is not compatible with this version of compiler"
            }

            val extensionStorage = configuration.extensionsStorage ?: return
            for (registrar in configuration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)) {
                with(registrar) { extensionStorage.registerExtensions(configuration) }
            }
            configuration[TEST_ONLY_PLUGIN_REGISTRATION_CALLBACK]?.invoke(extensionStorage)
            configuration[TEST_ONLY_PROJECT_CONFIGURATION_CALLBACK]?.invoke(project)
            extensionStorage.registerInProject(project) { createErrorMessage(it) }
        }

        @JvmStatic
        @CoreEnvironmentDeprecation
        fun registerProjectExtensionPoints(area: ExtensionsArea) {
            registerExtensionPoint(area, PsiTreeChangePreprocessor.EP.name, PsiTreeChangePreprocessor::class.java)
            registerExtensionPoint(area, PsiElementFinder.EP.name, PsiElementFinder::class.java)
            registerExtensionPoint(area, JvmElementProvider.EP_NAME, JvmElementProvider::class.java)
        }

        // made public for Android Lint
        @JvmStatic
        @CoreEnvironmentDeprecation
        fun registerProjectServices(project: MockProject) {
            with(project) {
                registerService(JavaElementSourceFactory::class.java, JavaFixedElementSourceFactory::class.java)
                registerService(PsiJavaModuleModificationTracker::class.java, PsiJavaModuleModificationTracker::class.java)
                registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(this))
                if (getService(PsiNameHelper::class.java) == null) {
                    registerService(PsiNameHelper::class.java, PsiNameHelperImpl::class.java)
                }
            }
        }
    }
}
