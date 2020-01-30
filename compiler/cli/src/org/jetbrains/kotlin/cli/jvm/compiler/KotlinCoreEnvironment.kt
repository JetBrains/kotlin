/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.impl.ZipHandler
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
import org.jetbrains.kotlin.asJava.classes.FacadeCache
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.extensions.ShellExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.cli.jvm.JvmRuntimeVersionsConsistencyChecker
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.index.*
import org.jetbrains.kotlin.cli.jvm.javac.JavacWrapperRegistrar
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.translate.extensions.JsSyntheticTranslateExtension
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver
import org.jetbrains.kotlin.resolve.extensions.ExtraImportsProviderExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.zip.ZipFile
import javax.xml.stream.XMLInputFactory

class KotlinCoreEnvironment private constructor(
    val projectEnvironment: JavaCoreProjectEnvironment,
    initialConfiguration: CompilerConfiguration,
    configFiles: EnvironmentConfigFiles
) {

    class ProjectEnvironment(
        disposable: Disposable, applicationEnvironment: KotlinCoreApplicationEnvironment
    ) :
        KotlinCoreProjectEnvironment(disposable, applicationEnvironment) {

        private var extensionRegistered = false

        override fun preregisterServices() {
            registerProjectExtensionPoints(Extensions.getArea(project))
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
                    ServiceManager.getService(this, JavaFileManager::class.java) as CoreJavaFileManager
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

    val configuration: CompilerConfiguration = initialConfiguration.apply { setupJdkClasspathRoots(configFiles) }.copy()

    init {
        PersistentFSConstants::class.java.getDeclaredField("ourMaxIntellisenseFileSize")
            .apply { isAccessible = true }
            .setInt(null, FileUtilRt.LARGE_FOR_CONTENT_LOADING)

        val project = projectEnvironment.project

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        (projectEnvironment as? ProjectEnvironment)?.registerExtensionsFromPlugins(configuration)
        // otherwise consider that project environment is properly configured before passing to the environment
        // TODO: consider some asserts to check important extension points

        project.registerService(DeclarationProviderFactoryService::class.java, CliDeclarationProviderFactoryService(sourceFiles))

        val isJvm = configFiles == EnvironmentConfigFiles.JVM_CONFIG_FILES
        project.registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(isJvm))

        registerProjectServicesForCLI(projectEnvironment)

        registerProjectServices(projectEnvironment.project)

        for (extension in CompilerConfigurationExtension.getInstances(project)) {
            extension.updateConfiguration(configuration)
        }

        sourceFiles += createKtFiles(project)

        collectAdditionalSources(project)

        sourceFiles.sortBy { it.virtualFile.path }

        val jdkHome = configuration.get(JVMConfigurationKeys.JDK_HOME)
        val jrtFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JRT_PROTOCOL)
        val javaModuleFinder = CliJavaModuleFinder(jdkHome?.path?.let { path ->
            jrtFileSystem?.findFileByPath(path + URLUtil.JAR_SEPARATOR)
        })

        val outputDirectory =
            configuration.get(JVMConfigurationKeys.MODULES)?.singleOrNull()?.getOutputDirectory()
                ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)?.absolutePath

        classpathRootsResolver = ClasspathRootsResolver(
            PsiManager.getInstance(project),
            messageCollector,
            configuration.getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES),
            this::contentRootToVirtualFile,
            javaModuleFinder,
            !configuration.getBoolean(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE),
            outputDirectory?.let(this::findLocalFile)
        )

        val (initialRoots, javaModules) =
            classpathRootsResolver.convertClasspathRoots(configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS))
        this.initialRoots.addAll(initialRoots)

        if (!configuration.getBoolean(JVMConfigurationKeys.SKIP_RUNTIME_VERSION_CHECK) && messageCollector != null) {
            JvmRuntimeVersionsConsistencyChecker.checkCompilerClasspathConsistency(
                messageCollector,
                configuration,
                initialRoots.mapNotNull { (file, type) -> if (type == JavaRoot.RootType.BINARY) file else null }
            )
        }

        val (roots, singleJavaFileRoots) =
            initialRoots.partition { (file) -> file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION }

        // REPL and kapt2 update classpath dynamically
        rootsIndex = JvmDependenciesDynamicCompoundIndex().apply {
            addIndex(JvmDependenciesIndexImpl(roots))
            updateClasspathFromRootsIndex(this)
        }

        (ServiceManager.getService(project, CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl).initialize(
            rootsIndex,
            packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            configuration.getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING)
        )

        project.registerService(
            JavaModuleResolver::class.java,
            CliJavaModuleResolver(classpathRootsResolver.javaModuleGraph, javaModules, javaModuleFinder.systemModules.toList())
        )

        val finderFactory = CliVirtualFileFinderFactory(rootsIndex)
        project.registerService(MetadataFinderFactory::class.java, finderFactory)
        project.registerService(VirtualFileFinderFactory::class.java, finderFactory)

        project.putUserData(APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY, fun(roots: List<File>) {
            updateClasspath(roots.map { JavaSourceRoot(it, null) })
        })
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
        val roots = rootDirs.map { KotlinSourceRoot(it.absolutePath, isCommon = false) }
        sourceFiles += createSourceFilesFromSourceRoots(configuration, project, roots)
    }

    fun createPackagePartProvider(scope: GlobalSearchScope): JvmPackagePartProvider {
        return JvmPackagePartProvider(configuration.languageVersionSettings, scope).apply {
            addRoots(initialRoots, configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY))
            packagePartProviders += this
            (ModuleAnnotationsResolver.getInstance(project) as CliModuleAnnotationsResolver).addPackagePartProvider(this)
        }
    }

    private val VirtualFile.javaFiles: List<VirtualFile>
        get() = mutableListOf<VirtualFile>().apply {
            VfsUtilCore.processFilesRecursively(this@javaFiles) { file ->
                if (file.fileType == JavaFileType.INSTANCE) {
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

    internal fun countLinesOfCode(sourceFiles: List<KtFile>): Int =
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
        val newRoots = classpathRootsResolver.convertClasspathRoots(contentRoots).roots

        if (packagePartProviders.isEmpty()) {
            initialRoots.addAll(newRoots)
        } else {
            for (packagePartProvider in packagePartProviders) {
                packagePartProvider.addRoots(newRoots, configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY))
            }
        }

        return rootsIndex.addNewIndexForRoots(newRoots)?.let { newIndex ->
            updateClasspathFromRootsIndex(newIndex)
            newIndex.indexedRoots.mapNotNull { (file) ->
                VfsUtilCore.virtualToIoFile(VfsUtilCore.getVirtualFileForJar(file) ?: file)
            }.toList()
        }.orEmpty()
    }

    private fun contentRootToVirtualFile(root: JvmContentRoot): VirtualFile? =
        when (root) {
            is JvmClasspathRoot ->
                if (root.file.isFile) findJarRoot(root.file) else findExistingRoot(root, "Classpath entry")
            is JvmModulePathRoot ->
                if (root.file.isFile) findJarRoot(root.file) else findExistingRoot(root, "Java module root")
            is JavaSourceRoot ->
                findExistingRoot(root, "Java source root")
            else ->
                throw IllegalStateException("Unexpected root: $root")
        }

    internal fun findLocalFile(path: String): VirtualFile? =
        applicationEnvironment.localFileSystem.findFileByPath(path)

    private fun findExistingRoot(root: JvmContentRoot, rootDescription: String): VirtualFile? {
        return findLocalFile(root.file.absolutePath).also {
            if (it == null) {
                report(STRONG_WARNING, "$rootDescription points to a non-existent location: ${root.file}")
            }
        }
    }

    private fun findJarRoot(file: File): VirtualFile? =
        applicationEnvironment.jarFileSystem.findFileByPath("$file${URLUtil.JAR_SEPARATOR}")

    private fun getSourceRootsCheckingForDuplicates(): List<KotlinSourceRoot> {
        val uniqueSourceRoots = hashSetOf<String>()
        val result = mutableListOf<KotlinSourceRoot>()

        for (root in configuration.kotlinSourceRoots) {
            if (!uniqueSourceRoots.add(root.path)) {
                report(STRONG_WARNING, "Duplicate source root: ${root.path}")
            }
            result.add(root)
        }

        return result
    }

    fun getSourceFiles(): List<KtFile> = sourceFiles

    private fun createKtFiles(project: Project): List<KtFile> =
        createSourceFilesFromSourceRoots(configuration, project, getSourceRootsCheckingForDuplicates())

    internal fun report(severity: CompilerMessageSeverity, message: String) = configuration.report(severity, message)

    companion object {
        private val LOG = Logger.getInstance(KotlinCoreEnvironment::class.java)

        private val APPLICATION_LOCK = Object()
        private var ourApplicationEnvironment: KotlinCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        @JvmStatic
        fun createForProduction(
            parentDisposable: Disposable, configuration: CompilerConfiguration, configFiles: EnvironmentConfigFiles
        ): KotlinCoreEnvironment {
            val appEnv = getOrCreateApplicationEnvironmentForProduction(parentDisposable, configuration)
            val projectEnv = ProjectEnvironment(parentDisposable, appEnv)
            val environment = KotlinCoreEnvironment(projectEnv, configuration, configFiles)

            synchronized(APPLICATION_LOCK) {
                ourProjectCount++
            }
            return environment
        }

        @JvmStatic
        fun createForProduction(
            projectEnvironment: JavaCoreProjectEnvironment, configuration: CompilerConfiguration, configFiles: EnvironmentConfigFiles
        ): KotlinCoreEnvironment {
            val environment = KotlinCoreEnvironment(projectEnvironment, configuration, configFiles)

            if (projectEnvironment.environment == applicationEnvironment) {
                // accounting for core environment disposing
                synchronized(APPLICATION_LOCK) {
                    ourProjectCount++
                }
            }
            return environment
        }

        @TestOnly
        @JvmStatic
        fun createForTests(
            parentDisposable: Disposable, initialConfiguration: CompilerConfiguration, extensionConfigs: EnvironmentConfigFiles
        ): KotlinCoreEnvironment {
            val configuration = initialConfiguration.copy()
            // Tests are supposed to create a single project and dispose it right after use
            val appEnv = createApplicationEnvironment(parentDisposable, configuration, unitTestMode = true)
            val projectEnv = ProjectEnvironment(parentDisposable, appEnv)
            return KotlinCoreEnvironment(projectEnv, configuration, extensionConfigs)
        }

        // used in the daemon for jar cache cleanup
        val applicationEnvironment: KotlinCoreApplicationEnvironment? get() = ourApplicationEnvironment

        fun getOrCreateApplicationEnvironmentForProduction(
            parentDisposable: Disposable, configuration: CompilerConfiguration
        ): KotlinCoreApplicationEnvironment {
            synchronized(APPLICATION_LOCK) {
                if (ourApplicationEnvironment == null) {
                    val disposable = Disposer.newDisposable()
                    ourApplicationEnvironment = createApplicationEnvironment(disposable, configuration, unitTestMode = false)
                    ourProjectCount = 0
                    Disposer.register(disposable, Disposable {
                        synchronized(APPLICATION_LOCK) {
                            ourApplicationEnvironment = null
                        }
                    })
                }
                // Disposing of the environment is unsafe in production then parallel builds are enabled, but turning it off universally
                // breaks a lot of tests, therefore it is disabled for production and enabled for tests
                if (System.getProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY).toBooleanLenient() != true) {
                    // JPS may run many instances of the compiler in parallel (there's an option for compiling independent modules in parallel in IntelliJ)
                    // All projects share the same ApplicationEnvironment, and when the last project is disposed, the ApplicationEnvironment is disposed as well
                    Disposer.register(parentDisposable, Disposable {
                        synchronized(APPLICATION_LOCK) {
                            if (--ourProjectCount <= 0) {
                                disposeApplicationEnvironment()
                            }
                        }
                    })
                }

                return ourApplicationEnvironment!!
            }
        }

        private fun disposeApplicationEnvironment() {
            synchronized(APPLICATION_LOCK) {
                val environment = ourApplicationEnvironment ?: return
                ourApplicationEnvironment = null
                Disposer.dispose(environment.parentDisposable)
                ZipHandler.clearFileAccessorCache()
            }
        }

        private fun createApplicationEnvironment(
            parentDisposable: Disposable, configuration: CompilerConfiguration, unitTestMode: Boolean
        ): KotlinCoreApplicationEnvironment {
            val applicationEnvironment = KotlinCoreApplicationEnvironment.create(parentDisposable, unitTestMode)

            registerApplicationExtensionPointsAndExtensionsFrom(configuration, "extensions/compiler.xml")

            registerApplicationServicesForCLI(applicationEnvironment)
            registerApplicationServices(applicationEnvironment)

            return applicationEnvironment
        }

        private fun registerApplicationExtensionPointsAndExtensionsFrom(configuration: CompilerConfiguration, configFilePath: String) {
            workaroundIbmJdkStaxReportCdataEventIssue()

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
                    ?: throw IllegalStateException(
                        "Unable to find extension point configuration $configFilePath " +
                                "(cp:\n  ${(Thread.currentThread().contextClassLoader as? UrlClassLoader)?.urls?.joinToString("\n  ") { it.file }})"
                    )

            registerExtensionPointAndExtensionsEx(pluginRoot, configFilePath, Extensions.getRootArea())
        }

        private fun workaroundIbmJdkStaxReportCdataEventIssue() {
            if (!SystemInfo.isIbmJvm) return

            // On IBM JDK, XMLInputFactory does not support "report-cdata-event" property, but JDOMUtil sets it unconditionally in the
            // static XML_INPUT_FACTORY field and fails with an exception (Logger.error throws exception in the compiler) if unsuccessful.
            // Until this is fixed in the platform, we workaround the issue by setting that field to a value that does not attempt
            // to set the unsupported property.
            // See IDEA-206446 for more information
            val field = JDOMUtil::class.java.getDeclaredField("XML_INPUT_FACTORY")
            field.isAccessible = true
            Field::class.java.getDeclaredField("modifiers")
                .apply { isAccessible = true }
                .setInt(field, field.modifiers and Modifier.FINAL.inv())
            field.set(null, object : NotNullLazyValue<XMLInputFactory>() {
                override fun compute(): XMLInputFactory {
                    val factory: XMLInputFactory = try {
                        // otherwise wst can be used (in tests/dev run)
                        val clazz = Class.forName("com.sun.xml.internal.stream.XMLInputFactoryImpl")
                        clazz.newInstance() as XMLInputFactory
                    } catch (e: Exception) {
                        // ok, use random
                        XMLInputFactory.newFactory()
                    }

                    factory.setProperty(XMLInputFactory.IS_COALESCING, true)
                    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
                    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
                    return factory
                }
            })
        }

        @JvmStatic
        @Suppress("MemberVisibilityCanPrivate") // made public for CLI Android Lint
        fun registerPluginExtensionPoints(project: MockProject) {
            ExpressionCodegenExtension.registerExtensionPoint(project)
            SyntheticResolveExtension.registerExtensionPoint(project)
            ClassBuilderInterceptorExtension.registerExtensionPoint(project)
            AnalysisHandlerExtension.registerExtensionPoint(project)
            PackageFragmentProviderExtension.registerExtensionPoint(project)
            StorageComponentContainerContributor.registerExtensionPoint(project)
            DeclarationAttributeAltererExtension.registerExtensionPoint(project)
            PreprocessedVirtualFileFactoryExtension.registerExtensionPoint(project)
            JsSyntheticTranslateExtension.registerExtensionPoint(project)
            CompilerConfigurationExtension.registerExtensionPoint(project)
            CollectAdditionalSourcesExtension.registerExtensionPoint(project)
            ExtraImportsProviderExtension.registerExtensionPoint(project)
            IrGenerationExtension.registerExtensionPoint(project)
            ScriptEvaluationExtension.registerExtensionPoint(project)
            ShellExtension.registerExtensionPoint(project)
            TypeResolutionInterceptor.registerExtensionPoint(project)
            CandidateInterceptor.registerExtensionPoint(project)
        }

        internal fun registerExtensionsFromPlugins(project: MockProject, configuration: CompilerConfiguration) {
            val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            for (registrar in configuration.getList(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)) {
                try {
                    registrar.registerProjectComponents(project, configuration)
                } catch (e: AbstractMethodError) {
                    val message = "The provided plugin ${registrar.javaClass.name} is not compatible with this version of compiler"
                    // Since the scripting plugin is often discovered in the compiler environment, it is often taken from the incompatible
                    // location, and in many cases this is not a fatal error, therefore strong warning is generated instead of exception
                    if (registrar.javaClass.simpleName == "ScriptingCompilerConfigurationComponentRegistrar") {
                        messageCollector?.report(STRONG_WARNING, "Default scripting plugin is disabled: $message")
                    } else {
                        throw IllegalStateException(message, e)
                    }
                }
            }
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
            }
        }

        @JvmStatic
        fun registerProjectExtensionPoints(area: ExtensionsArea) {
            CoreApplicationEnvironment.registerExtensionPoint(
                area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor::class.java
            )
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder::class.java)

            IdeaExtensionPoints.registerVersionSpecificProjectExtensionPoints(area)
        }

        // made public for Upsource
        @JvmStatic
        @Deprecated("Use registerProjectServices(project) instead.", ReplaceWith("registerProjectServices(projectEnvironment.project)"))
        fun registerProjectServices(projectEnvironment: JavaCoreProjectEnvironment, messageCollector: MessageCollector?) {
            registerProjectServices(projectEnvironment.project)
        }

        // made public for Android Lint
        @JvmStatic
        fun registerProjectServices(project: MockProject) {
            with(project) {
                registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(this))
                registerService(FacadeCache::class.java, FacadeCache(this))
                registerService(ModuleAnnotationsResolver::class.java, CliModuleAnnotationsResolver())
            }
        }

        private fun registerProjectServicesForCLI(@Suppress("UNUSED_PARAMETER") projectEnvironment: JavaCoreProjectEnvironment) {
            /**
             * Note that Kapt may restart code analysis process, and CLI services should be aware of that.
             * Use PsiManager.getModificationTracker() to ensure that all the data you cached is still valid.
             */
        }

        // made public for Android Lint
        @JvmStatic
        fun registerKotlinLightClassSupport(project: MockProject) {
            with(project) {
                val traceHolder = CliTraceHolder()
                val cliLightClassGenerationSupport = CliLightClassGenerationSupport(traceHolder)
                val kotlinAsJavaSupport = CliKotlinAsJavaSupport(this, traceHolder)
                registerService(LightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                registerService(CliLightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                registerService(KotlinAsJavaSupport::class.java, kotlinAsJavaSupport)
                registerService(CodeAnalyzerInitializer::class.java, traceHolder)

                val area = Extensions.getArea(this)

                area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(JavaElementFinder(this, kotlinAsJavaSupport))
                area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(
                    PsiElementFinderImpl(this, ServiceManager.getService(this, JavaFileManager::class.java))
                )
            }
        }

        private fun CompilerConfiguration.setupJdkClasspathRoots(configFiles: EnvironmentConfigFiles) {
            if (getBoolean(JVMConfigurationKeys.NO_JDK)) return

            val jvmTarget = configFiles == EnvironmentConfigFiles.JVM_CONFIG_FILES
            if (!jvmTarget) return

            val jdkHome = get(JVMConfigurationKeys.JDK_HOME)
            val (javaRoot, classesRoots) = if (jdkHome == null) {
                val javaHome = File(System.getProperty("java.home"))
                put(JVMConfigurationKeys.JDK_HOME, javaHome)

                javaHome to PathUtil.getJdkClassesRootsFromCurrentJre()
            } else {
                jdkHome to PathUtil.getJdkClassesRoots(jdkHome)
            }

            if (!CoreJrtFileSystem.isModularJdk(javaRoot)) {
                if (classesRoots.isEmpty()) {
                    report(ERROR, "No class roots are found in the JDK path: $javaRoot")
                } else {
                    addJvmSdkRoots(classesRoots)
                }
            }
        }
    }
}