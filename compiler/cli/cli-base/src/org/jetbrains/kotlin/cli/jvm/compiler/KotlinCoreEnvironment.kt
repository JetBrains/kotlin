/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.pom.java.InternalPersistentJavaLanguageLevelReaderService
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.JavaClassSupers
import com.intellij.util.io.URLUtil
import com.intellij.util.lang.UrlClassLoader
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.extensions.ShellExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.index.*
import org.jetbrains.kotlin.cli.jvm.javac.JavacWrapperRegistrar
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.registerInProject
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaFixedElementSourceFactory
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension
import org.jetbrains.kotlin.resolve.extensions.ExtraImportsProviderExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.SyntheticJavaResolveExtension
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.DescriptorSerializerPlugin
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.nio.file.FileSystems
import java.util.zip.ZipFile

class KotlinCoreEnvironment private constructor(
    val projectEnvironment: ProjectEnvironment,
    val configuration: CompilerConfiguration,
    configFiles: EnvironmentConfigFiles
) {

    class ProjectEnvironment(
        disposable: Disposable,
        applicationEnvironment: KotlinCoreApplicationEnvironment,
        configuration: CompilerConfiguration
    ) :
        KotlinCoreProjectEnvironment(disposable, applicationEnvironment) {

        val jarFileSystem: VirtualFileSystem

        init {
            val messageCollector = configuration.messageCollector

            setIdeaIoUseFallback()

            val useFastJarFSFlag: Boolean? = configuration.get(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM)
            val useK2 =
                configuration.getBoolean(CommonConfigurationKeys.USE_FIR) || configuration.languageVersionSettings.languageVersion.usesK2

            when {
                useFastJarFSFlag == true && !useK2 -> {
                    messageCollector.report(
                        STRONG_WARNING,
                        "Using new faster version of JAR FS: it should make your build faster, " +
                                "but the new implementation is not thoroughly tested with language versions below 2.0"
                    )
                }
                useFastJarFSFlag == false && useK2 -> {
                    messageCollector.report(
                        INFO,
                        "Using outdated version of JAR FS: it might make your build slower"
                    )
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
                        messageCollector.report(
                            STRONG_WARNING,
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
                                messageCollector.report(
                                    STRONG_WARNING,
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
                registerPluginExtensionPoints(project)
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

                registerKotlinLightClassSupport(project)

                registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManager())
                registerService(InferredAnnotationsManager::class.java, MockInferredAnnotationsManager())
            }

            super.registerJavaPsiFacade()
        }
    }

    private val sourceFiles = mutableListOf<KtFile>()
    private val rootsIndex: JvmDependenciesDynamicCompoundIndex
    private val packagePartProviders = mutableListOf<JvmPackagePartProvider>()

    private val classpathRootsResolver: ClasspathRootsResolver
    private val initialRoots = ArrayList<JavaRoot>()

    init {
        projectEnvironment.configureProjectEnvironment(configuration, configFiles)
        val project = projectEnvironment.project
        project.registerService(DeclarationProviderFactoryService::class.java, CliDeclarationProviderFactoryService(sourceFiles))

        sourceFiles += createSourceFilesFromSourceRoots(
            configuration, project,
            getSourceRootsCheckingForDuplicates(configuration, configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY])
        )

        collectAdditionalSources(project)

        sourceFiles.sortBy { it.virtualFile.path }

        val javaFileManager = project.getService(CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

        val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val jdkHome = configuration.get(JVMConfigurationKeys.JDK_HOME)
        val releaseTarget = configuration.get(JVMConfigurationKeys.JDK_RELEASE)
        val javaModuleFinder = CliJavaModuleFinder(
            jdkHome,
            messageCollector,
            javaFileManager,
            project,
            releaseTarget
        )

        val outputDirectory =
            configuration.get(JVMConfigurationKeys.MODULES)?.singleOrNull()?.getOutputDirectory()
                ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)?.absolutePath

        val contentRoots = configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)

        classpathRootsResolver = ClasspathRootsResolver(
            PsiManager.getInstance(project),
            messageCollector,
            configuration.getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES),
            this::contentRootToVirtualFile,
            javaModuleFinder,
            !configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE),
            outputDirectory?.let(this::findLocalFile),
            javaFileManager,
            releaseTarget,
            hasKotlinSources = contentRoots.any { it is KotlinSourceRoot },
        )

        val (initialRoots, javaModules) = classpathRootsResolver.convertClasspathRoots(contentRoots)
        this.initialRoots.addAll(initialRoots)

        val (roots, singleJavaFileRoots) =
            initialRoots.partition { (file) -> file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION }

        // REPL and kapt2 update classpath dynamically
        rootsIndex = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true).apply {
            addIndex(JvmDependenciesIndexImpl(roots, shouldOnlyFindFirstClass = true))
            updateClasspathFromRootsIndex(this)
        }

        javaFileManager.initialize(
            rootsIndex,
            packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            configuration.getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING)
        )

        project.registerService(
            JavaModuleResolver::class.java,
            CliJavaModuleResolver(classpathRootsResolver.javaModuleGraph, javaModules, javaModuleFinder.systemModules.toList(), project)
        )

        val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, releaseTarget != null)
        project.registerService(VirtualFileFinderFactory::class.java, fileFinderFactory)
        project.registerService(MetadataFinderFactory::class.java, CliMetadataFinderFactory(fileFinderFactory))

        project.putUserData(APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY, fun(roots: List<File>) {
            updateClasspath(roots.map { JavaSourceRoot(it, null) })
        })

        project.setupHighestLanguageLevel()
    }

    private fun collectAdditionalSources(project: MockProject) {
        var unprocessedSources: Collection<KtFile> = sourceFiles
        val processedSources = HashSet<KtFile>()
        val processedSourcesByExtension = HashMap<CollectAdditionalSourcesExtension, Collection<KtFile>>()
        // repeat feeding extensions with sources while new sources a being added
        var sourceCollectionIterations = 0
        while (unprocessedSources.isNotEmpty()) {
            if (sourceCollectionIterations++ > 10) { // TODO: consider using some appropriate global constant
                throw IllegalStateException("Unable to collect additional sources in reasonable number of iterations")
            }
            processedSources.addAll(unprocessedSources)
            val allNewSources = ArrayList<KtFile>()
            for (extension in CollectAdditionalSourcesExtension.getInstances(project)) {
                // do not feed the extension with the sources it returned on the previous iteration
                val sourcesToProcess = unprocessedSources - (processedSourcesByExtension[extension] ?: emptyList())
                val newSources = extension.collectAdditionalSourcesAndUpdateConfiguration(sourcesToProcess, configuration, project)
                if (newSources.isNotEmpty()) {
                    allNewSources.addAll(newSources)
                    processedSourcesByExtension[extension] = newSources
                }
            }
            unprocessedSources = allNewSources.filterNot { processedSources.contains(it) }.distinct()
            sourceFiles += unprocessedSources
        }
    }

    fun addKotlinSourceRoots(rootDirs: List<File>) {
        val roots = rootDirs.map { KotlinSourceRoot(it.absolutePath, isCommon = false, hmppModuleName = null) }
        sourceFiles += createSourceFilesFromSourceRoots(configuration, project, roots).toSet() - sourceFiles
    }

    fun createPackagePartProvider(scope: GlobalSearchScope): JvmPackagePartProvider {
        return JvmPackagePartProvider(configuration.languageVersionSettings, scope).apply {
            addRoots(initialRoots, configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY))
            packagePartProviders += this
        }
    }

    private val VirtualFile.javaFiles: List<VirtualFile>
        get() = mutableListOf<VirtualFile>().apply {
            VfsUtilCore.processFilesRecursively(this@javaFiles) { file ->
                if (file.extension == JavaFileType.DEFAULT_EXTENSION || file.fileType == JavaFileType.INSTANCE) {
                    add(file)
                }
                true
            }
        }

    private val allJavaFiles: List<File>
        get() = configuration.javaSourceRoots
            .mapNotNull(this::findLocalFile)
            .flatMap { it.javaFiles }
            .map { File(it.canonicalPath) }

    fun registerJavac(
        javaFiles: List<File> = allJavaFiles,
        kotlinFiles: List<KtFile> = sourceFiles,
        arguments: Array<String>? = null,
        bootClasspath: List<File>? = null,
        sourcePath: List<File>? = null
    ): Boolean {
        return JavacWrapperRegistrar.registerJavac(
            projectEnvironment.project, configuration, javaFiles, kotlinFiles, arguments, bootClasspath, sourcePath,
            LightClassGenerationSupport.getInstance(project), packagePartProviders
        )
    }

    private val applicationEnvironment: CoreApplicationEnvironment
        get() = projectEnvironment.environment

    val project: Project
        get() = projectEnvironment.project

    fun countLinesOfCode(sourceFiles: List<KtFile>): Int =
        sourceFiles.sumBy { sourceFile ->
            val text = sourceFile.text
            StringUtil.getLineBreakCount(text) + (if (StringUtil.endsWithLineBreak(text)) 0 else 1)
        }

    private fun updateClasspathFromRootsIndex(index: JvmDependenciesIndex) {
        index.indexedRoots.forEach {
            projectEnvironment.addSourcesToClasspath(it.file)
        }
    }

    fun updateClasspath(contentRoots: List<ContentRoot>): List<File>? {
        // TODO: add new Java modules to CliJavaModuleResolver
        val newRoots = classpathRootsResolver.convertClasspathRoots(contentRoots).roots - initialRoots

        if (packagePartProviders.isEmpty()) {
            initialRoots.addAll(newRoots)
        } else {
            for (packagePartProvider in packagePartProviders) {
                packagePartProvider.addRoots(newRoots, configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY))
            }
        }

        configuration.addAll(CLIConfigurationKeys.CONTENT_ROOTS, contentRoots - configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS))

        return rootsIndex.addNewIndexForRoots(newRoots)?.let { newIndex ->
            updateClasspathFromRootsIndex(newIndex)
            newIndex.indexedRoots.mapNotNull { (file) ->
                VfsUtilCore.virtualToIoFile(VfsUtilCore.getVirtualFileForJar(file) ?: file)
            }.toList()
        }.orEmpty()
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
                report(STRONG_WARNING, "$rootDescription points to a non-existent location: ${root.file}")
            }
        }
    }

    private fun findJarRoot(file: File): VirtualFile? =
        projectEnvironment.jarFileSystem.findFileByPath("$file${URLUtil.JAR_SEPARATOR}")

    fun getSourceFiles(): List<KtFile> =
        ProcessSourcesBeforeCompilingExtension.getInstances(project)
            .fold(sourceFiles as Collection<KtFile>) { files, extension ->
                extension.processSources(files, configuration)
            }.toList()

    internal fun report(severity: CompilerMessageSeverity, message: String) = configuration.report(severity, message)

    companion object {
        private val LOG = Logger.getInstance(KotlinCoreEnvironment::class.java)

        @PublishedApi
        internal val APPLICATION_LOCK = Object()

        private var ourApplicationEnvironment: KotlinCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        inline fun <R> underApplicationLock(action: () -> R): R =
            synchronized(APPLICATION_LOCK) { action() }

        @JvmStatic
        fun createForProduction(
            projectDisposable: Disposable,
            configuration: CompilerConfiguration,
            configFiles: EnvironmentConfigFiles,
        ): KotlinCoreEnvironment {
            setupIdeaStandaloneExecution()
            val appEnv = getOrCreateApplicationEnvironmentForProduction(projectDisposable, configuration)
            val projectEnv = ProjectEnvironment(projectDisposable, appEnv, configuration)
            val environment = KotlinCoreEnvironment(projectEnv, configuration, configFiles)

            return environment
        }

        @JvmStatic
        fun createForProduction(
            projectEnvironment: ProjectEnvironment, configuration: CompilerConfiguration, configFiles: EnvironmentConfigFiles
        ): KotlinCoreEnvironment {
            return KotlinCoreEnvironment(projectEnvironment, configuration, configFiles)
        }

        @TestOnly
        @JvmStatic
        fun createForTests(
            parentDisposable: Disposable, initialConfiguration: CompilerConfiguration, extensionConfigs: EnvironmentConfigFiles
        ): KotlinCoreEnvironment {
            val configuration = initialConfiguration.copy()
            // Tests are supposed to create a single project and dispose it right after use
            val appEnv =
                createApplicationEnvironment(
                    parentDisposable,
                    configuration,
                    KotlinCoreApplicationEnvironmentMode.UnitTest,
                )
            val projectEnv = ProjectEnvironment(parentDisposable, appEnv, configuration)
            return KotlinCoreEnvironment(projectEnv, configuration, extensionConfigs)
        }

        @TestOnly
        @JvmStatic
        fun createForParallelTests(
            projectDisposable: Disposable,
            initialConfiguration: CompilerConfiguration,
            extensionConfigs: EnvironmentConfigFiles,
        ): KotlinCoreEnvironment {
            val configuration = initialConfiguration.copy()
            val appEnv = getOrCreateApplicationEnvironmentForTests(projectDisposable, configuration)
            val projectEnv = ProjectEnvironment(projectDisposable, appEnv, configuration)
            return KotlinCoreEnvironment(projectEnv, configuration, extensionConfigs)
        }

        @TestOnly
        @JvmStatic
        fun createForTests(
            projectEnvironment: ProjectEnvironment, initialConfiguration: CompilerConfiguration, extensionConfigs: EnvironmentConfigFiles
        ): KotlinCoreEnvironment {
            return KotlinCoreEnvironment(projectEnvironment, initialConfiguration, extensionConfigs)
        }

        @TestOnly
        fun createProjectEnvironmentForTests(projectDisposable: Disposable, configuration: CompilerConfiguration): ProjectEnvironment {
            val appEnv = createApplicationEnvironment(
                projectDisposable,
                configuration,
                KotlinCoreApplicationEnvironmentMode.UnitTest,
            )
            return ProjectEnvironment(projectDisposable, appEnv, configuration)
        }

        // used in the daemon for jar cache cleanup
        val applicationEnvironment: KotlinCoreApplicationEnvironment? get() = ourApplicationEnvironment

        fun getOrCreateApplicationEnvironmentForProduction(
            projectDisposable: Disposable,
            configuration: CompilerConfiguration,
        ): KotlinCoreApplicationEnvironment = getOrCreateApplicationEnvironment(
            projectDisposable,
            configuration,
            KotlinCoreApplicationEnvironmentMode.Production,
        )

        fun getOrCreateApplicationEnvironmentForTests(
            projectDisposable: Disposable,
            configuration: CompilerConfiguration,
        ): KotlinCoreApplicationEnvironment = getOrCreateApplicationEnvironment(
            projectDisposable,
            configuration,
            KotlinCoreApplicationEnvironmentMode.UnitTest,
        )

        fun getOrCreateApplicationEnvironment(
            projectDisposable: Disposable,
            configuration: CompilerConfiguration,
            environmentMode: KotlinCoreApplicationEnvironmentMode,
        ): KotlinCoreApplicationEnvironment {
            synchronized(APPLICATION_LOCK) {
                if (ourApplicationEnvironment == null) {
                    val disposable = Disposer.newDisposable("Disposable for the KotlinCoreApplicationEnvironment")
                    ourApplicationEnvironment =
                        createApplicationEnvironment(
                            disposable,
                            configuration,
                            environmentMode,
                        )
                    ourProjectCount = 0
                    Disposer.register(disposable, Disposable {
                        synchronized(APPLICATION_LOCK) {
                            ourApplicationEnvironment = null
                        }
                    })
                }
                try {
                    val disposeAppEnv =
                        CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value.toBooleanLenient() != true
                    // Disposer uses identity of passed object to deduplicate registered disposables
                    // We should everytime pass new instance to avoid un-registering from previous one
                    @Suppress("ObjectLiteralToLambda")
                    Disposer.register(projectDisposable, object : Disposable {
                        override fun dispose() {
                            synchronized(APPLICATION_LOCK) {
                                // Build-systems may run many instances of the compiler in parallel
                                // All projects share the same ApplicationEnvironment, and when the last project is disposed,
                                // the ApplicationEnvironment is disposed as well
                                if (--ourProjectCount <= 0) {
                                    // Do not use this property unless you sure need it, causes Application to MEMORY LEAK
                                    // Only valid use-case is when Application should be cached to avoid
                                    // initialization costs
                                    if (disposeAppEnv) {
                                        disposeApplicationEnvironment()
                                    } else {
                                        ourApplicationEnvironment?.idleCleanup()
                                    }
                                }
                            }
                        }
                    })
                } finally {
                    ourProjectCount++
                }

                return ourApplicationEnvironment!!
            }
        }

        /**
         * This method is also used in Gradle after configuration phase finished.
         */
        @JvmStatic
        fun disposeApplicationEnvironment() {
            synchronized(APPLICATION_LOCK) {
                val environment = ourApplicationEnvironment ?: return
                ourApplicationEnvironment = null
                Disposer.dispose(environment.parentDisposable)
                resetApplicationManager(environment.application)
                ZipHandler.clearFileAccessorCache()
            }
        }

        /**
         * Resets the application managed by [ApplicationManager] to `null`. If [applicationToReset] is specified, [resetApplicationManager]
         * will only reset the application if it's the expected one. Otherwise, the application will already have been changed to another
         * application. For example, application disposal can trigger one of the disposables registered via
         * [ApplicationManager.setApplication], which reset the managed application to the previous application.
         */
        @JvmStatic
        fun resetApplicationManager(applicationToReset: Application? = null) {
            val currentApplication = ApplicationManager.getApplication() ?: return
            if (applicationToReset != null && applicationToReset != currentApplication) {
                return
            }

            try {
                val ourApplicationField = ApplicationManager::class.java.getDeclaredField("ourApplication")
                ourApplicationField.isAccessible = true
                ourApplicationField.set(null, null)
            } catch (exception: Exception) {
                // Resetting the application manager is not critical in a production context. If the reflective access fails, we shouldn't
                // expose the user to the failure.
                if (currentApplication.isUnitTestMode) {
                    throw exception
                }
            }
        }

        @JvmStatic
        fun ProjectEnvironment.configureProjectEnvironment(
            configuration: CompilerConfiguration,
            configFiles: EnvironmentConfigFiles
        ) {
            PersistentFSConstants::class.java.getDeclaredField("ourMaxIntellisenseFileSize")
                .apply { isAccessible = true }
                .setInt(null, FileUtilRt.LARGE_FOR_CONTENT_LOADING)

            registerExtensionsFromPlugins(configuration)
            // otherwise consider that project environment is properly configured before passing to the environment
            // TODO: consider some asserts to check important extension points

            val isJvm = configFiles == EnvironmentConfigFiles.JVM_CONFIG_FILES
            project.registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(isJvm))

            registerProjectServicesForCLI(this)

            registerProjectServices(project)

            for (extension in CompilerConfigurationExtension.getInstances(project)) {
                extension.updateConfiguration(configuration)
            }
        }

        private fun createApplicationEnvironment(
            parentDisposable: Disposable,
            configuration: CompilerConfiguration,
            environmentMode: KotlinCoreApplicationEnvironmentMode,
        ): KotlinCoreApplicationEnvironment {
            val applicationEnvironment = KotlinCoreApplicationEnvironment.create(parentDisposable, environmentMode)

            registerApplicationExtensionPointsAndExtensionsFrom(configuration, "extensions/compiler.xml")

            registerApplicationServicesForCLI(applicationEnvironment)
            registerApplicationServices(applicationEnvironment)

            return applicationEnvironment
        }

        private fun registerApplicationExtensionPointsAndExtensionsFrom(configuration: CompilerConfiguration, configFilePath: String) {
            fun File.hasConfigFile(configFile: String): Boolean =
                if (isDirectory) File(this, "META-INF" + File.separator + configFile).exists()
                else try {
                    ZipFile(this).use {
                        it.getEntry("META-INF/$configFile") != null
                    }
                } catch (e: Throwable) {
                    false
                }

            val pluginRoot: File =
                configuration.get(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT)?.let(::File)
                    ?: PathUtil.getResourcePathForClass(this::class.java).takeIf { it.hasConfigFile(configFilePath) }
                    // hack for load extensions when compiler run directly from project directory (e.g. in tests)
                    ?: File("compiler/cli/cli-common/resources").takeIf { it.hasConfigFile(configFilePath) }
                    ?: configuration.get(CLIConfigurationKeys.PATH_TO_KOTLIN_COMPILER_JAR)?.takeIf { it.hasConfigFile(configFilePath) }
                    ?: throw IllegalStateException(
                        "Unable to find extension point configuration $configFilePath " +
                                "(cp:\n  ${(Thread.currentThread().contextClassLoader as? UrlClassLoader)?.urls?.joinToString("\n  ") { it.file }})"
                    )

            CoreApplicationEnvironment.registerExtensionPointAndExtensions(
                FileSystems.getDefault().getPath(pluginRoot.path),
                configFilePath,
                ApplicationManager.getApplication().extensionArea
            )
        }

        @JvmStatic
        @OptIn(InternalNonStableExtensionPoints::class)
        @Suppress("MemberVisibilityCanPrivate") // made public for CLI Android Lint
        fun registerPluginExtensionPoints(project: MockProject) {
            SyntheticResolveExtension.registerExtensionPoint(project)
            SyntheticJavaResolveExtension.registerExtensionPoint(project)
            @Suppress("DEPRECATION_ERROR")
            org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension.registerExtensionPoint(project)
            ClassGeneratorExtension.registerExtensionPoint(project)
            ClassFileFactoryFinalizerExtension.registerExtensionPoint(project)
            AnalysisHandlerExtension.registerExtensionPoint(project)
            PackageFragmentProviderExtension.registerExtensionPoint(project)
            StorageComponentContainerContributor.registerExtensionPoint(project)
            DeclarationAttributeAltererExtension.registerExtensionPoint(project)
            PreprocessedVirtualFileFactoryExtension.registerExtensionPoint(project)
            CompilerConfigurationExtension.registerExtensionPoint(project)
            CollectAdditionalSourcesExtension.registerExtensionPoint(project)
            ProcessSourcesBeforeCompilingExtension.registerExtensionPoint(project)
            ExtraImportsProviderExtension.registerExtensionPoint(project)
            IrGenerationExtension.registerExtensionPoint(project)
            ScriptEvaluationExtension.registerExtensionPoint(project)
            ShellExtension.registerExtensionPoint(project)
            TypeResolutionInterceptor.registerExtensionPoint(project)
            CandidateInterceptor.registerExtensionPoint(project)
            DescriptorSerializerPlugin.registerExtensionPoint(project)
            FirExtensionRegistrarAdapter.registerExtensionPoint(project)
            TypeAttributeTranslatorExtension.registerExtensionPoint(project)
            AssignResolutionAltererExtension.registerExtensionPoint(project)
            FirAnalysisHandlerExtension.registerExtensionPoint(project)
            DiagnosticSuppressor.registerExtensionPoint(project)
        }

        internal fun registerExtensionsFromPlugins(project: MockProject, configuration: CompilerConfiguration) {
            fun createErrorMessage(extension: Any): String {
                return "The provided plugin ${extension.javaClass.name} is not compatible with this version of compiler"
            }

            val messageCollector = configuration.messageCollector

            for (registrar in configuration.getList(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)) {
                try {
                    registrar.registerProjectComponents(project, configuration)
                } catch (e: AbstractMethodError) {
                    val message = createErrorMessage(registrar)
                    // Since the scripting plugin is often discovered in the compiler environment, it is often taken from the incompatible
                    // location, and in many cases this is not a fatal error, therefore strong warning is generated instead of exception
                    if (registrar.javaClass.simpleName == "ScriptingCompilerConfigurationComponentRegistrar") {
                        messageCollector.report(STRONG_WARNING, "Default scripting plugin is disabled: $message")
                    } else {
                        val errorMessageWithStackTrace = "$message.\n" +
                                e.stackTraceToString().lines().take(6).joinToString("\n")
                        messageCollector.report(ERROR, errorMessageWithStackTrace)
                    }
                }
            }

            val extensionStorage = CompilerPluginRegistrar.ExtensionStorage()
            for (registrar in configuration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)) {
                with(registrar) { extensionStorage.registerExtensions(configuration) }
            }
            extensionStorage.registerInProject(project) { createErrorMessage(it) }
        }

        private fun registerApplicationServicesForCLI(applicationEnvironment: KotlinCoreApplicationEnvironment) {
            // ability to get text from annotations xml files
            applicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml")
            applicationEnvironment.registerParserDefinition(JavaParserDefinition())
        }

        // made public for Upsource
        @Suppress("MemberVisibilityCanBePrivate")
        @JvmStatic
        fun registerApplicationServices(applicationEnvironment: KotlinCoreApplicationEnvironment) {
            with(applicationEnvironment) {
                registerFileType(KotlinFileType.INSTANCE, "kt")
                registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
                registerParserDefinition(KotlinParserDefinition())
                application.registerService(KotlinBinaryClassCache::class.java, KotlinBinaryClassCache())
                application.registerService(JavaClassSupers::class.java, JavaClassSupersImpl::class.java)
                application.registerService(TransactionGuard::class.java, TransactionGuardImpl::class.java)
                application.registerService(VirtualFileSetFactory::class.java, getCompactVirtualFileSetFactory())
                application.registerService(InternalPersistentJavaLanguageLevelReaderService::class.java, InternalPersistentJavaLanguageLevelReaderService.DefaultImpl())
            }
        }

        @JvmStatic
        fun registerProjectExtensionPoints(area: ExtensionsArea) {
            CoreApplicationEnvironment.registerExtensionPoint(
                area, PsiTreeChangePreprocessor.EP.name, PsiTreeChangePreprocessor::class.java
            )
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP.name, PsiElementFinder::class.java)

            IdeaExtensionPoints.registerVersionSpecificProjectExtensionPoints(area)
        }

        // made public for Upsource
        @JvmStatic
        @Deprecated("Use registerProjectServices(project) instead.", ReplaceWith("registerProjectServices(projectEnvironment.project)"))
        fun registerProjectServices(
            projectEnvironment: JavaCoreProjectEnvironment,
            @Suppress("UNUSED_PARAMETER") messageCollector: MessageCollector?
        ) {
            registerProjectServices(projectEnvironment.project)
        }

        // made public for Android Lint
        @JvmStatic
        fun registerProjectServices(project: MockProject) {
            with(project) {
                registerService(JavaElementSourceFactory::class.java, JavaFixedElementSourceFactory::class.java)
                registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(this))
            }
        }

        fun registerProjectServicesForCLI(@Suppress("UNUSED_PARAMETER") projectEnvironment: JavaCoreProjectEnvironment) {
            /**
             * Note that Kapt may restart code analysis process, and CLI services should be aware of that.
             * Use PsiManager.getModificationTracker() to ensure that all the data you cached is still valid.
             */
        }

        // made public for Android Lint
        @JvmStatic
        fun registerKotlinLightClassSupport(project: MockProject) {
            with(project) {
                val traceHolder = CliTraceHolder(project)
                val cliLightClassGenerationSupport = CliLightClassGenerationSupport(traceHolder, project)
                val kotlinAsJavaSupport = CliKotlinAsJavaSupport(project, traceHolder)
                registerService(LightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                registerService(CliLightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                registerService(KotlinAsJavaSupport::class.java, kotlinAsJavaSupport)
                registerService(CodeAnalyzerInitializer::class.java, traceHolder)

                // We don't pass Disposable because in some tests, we manually unregister these extensions, and that leads to LOG.error
                // exception from `ExtensionPointImpl.doRegisterExtension`, because the registered extension can no longer be found
                // when the project is being disposed.
                // For example, see the `unregisterExtension` call in `GenerationUtils.compileFilesUsingFrontendIR`.
                // TODO: refactor this to avoid registering unneeded extensions in the first place, and avoid using deprecated API. (KT-64296)
                @Suppress("DEPRECATION")
                PsiElementFinder.EP.getPoint(project).registerExtension(JavaElementFinder(this))
                @Suppress("DEPRECATION")
                PsiElementFinder.EP.getPoint(project).registerExtension(PsiElementFinderImpl(this))
            }
        }
    }
}
