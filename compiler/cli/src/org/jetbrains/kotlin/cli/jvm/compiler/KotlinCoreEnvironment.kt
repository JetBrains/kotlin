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

import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.*
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.MetaLanguage
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.psi.FileContextProvider
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.augment.TypeAnnotationModifier
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.psi.util.JavaClassSupers
import com.intellij.util.io.URLUtil
import com.intellij.util.lang.UrlClassLoader
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.script.CliScriptDefinitionProvider
import org.jetbrains.kotlin.cli.common.script.CliScriptDependenciesProvider
import org.jetbrains.kotlin.cli.common.script.CliScriptReportSink
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
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.script.ScriptReportSink
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.zip.ZipFile

class KotlinCoreEnvironment private constructor(
    parentDisposable: Disposable,
    applicationEnvironment: JavaCoreApplicationEnvironment,
    initialConfiguration: CompilerConfiguration,
    configFiles: EnvironmentConfigFiles
) {
    private val projectEnvironment: JavaCoreProjectEnvironment =
        object : KotlinCoreProjectEnvironment(parentDisposable, applicationEnvironment) {
            override fun preregisterServices() {
                registerProjectExtensionPoints(Extensions.getArea(project))
            }

            override fun registerJavaPsiFacade() {
                with(project) {
                    registerService(
                        CoreJavaFileManager::class.java,
                        ServiceManager.getService(this, JavaFileManager::class.java) as CoreJavaFileManager
                    )

                    val traceHolder = CliTraceHolder()
                    val cliLightClassGenerationSupport = CliLightClassGenerationSupport(traceHolder)
                    val kotlinAsJavaSupport = CliKotlinAsJavaSupport(this, traceHolder)
                    registerService(LightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                    registerService(CliLightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                    registerService(KotlinAsJavaSupport::class.java, kotlinAsJavaSupport)
                    registerService(CodeAnalyzerInitializer::class.java, traceHolder)

                    registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManager())
                    registerService(InferredAnnotationsManager::class.java, MockInferredAnnotationsManager())

                    val area = Extensions.getArea(this)

                    area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(JavaElementFinder(this, kotlinAsJavaSupport))
                    area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(
                        PsiElementFinderImpl(this, ServiceManager.getService(this, JavaFileManager::class.java))
                    )
                }

                super.registerJavaPsiFacade()
            }
        }

    private val sourceFiles = mutableListOf<KtFile>()
    private val rootsIndex: JvmDependenciesDynamicCompoundIndex
    private val packagePartProviders = mutableListOf<JvmPackagePartProvider>()

    private val classpathRootsResolver: ClasspathRootsResolver
    private val initialRoots: List<JavaRoot>

    val configuration: CompilerConfiguration = initialConfiguration.apply { setupJdkClasspathRoots(configFiles) }.copy()

    init {
        PersistentFSConstants::class.java.getDeclaredField("ourMaxIntellisenseFileSize")
            .apply { isAccessible = true }
            .setInt(null, FileUtilRt.LARGE_FOR_CONTENT_LOADING)

        val project = projectEnvironment.project

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
        IrGenerationExtension.registerExtensionPoint(project)

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

        project.registerService(DeclarationProviderFactoryService::class.java, CliDeclarationProviderFactoryService(sourceFiles))

        val isJvm = configFiles == EnvironmentConfigFiles.JVM_CONFIG_FILES
        project.registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(isJvm))

        registerProjectServicesForCLI(projectEnvironment)

        registerProjectServices(projectEnvironment, messageCollector)

        for (extension in CompilerConfigurationExtension.getInstances(project)) {
            extension.updateConfiguration(configuration)
        }

        // If not disabled explicitly, we should always support at least the standard script definition
        if (!configuration.getBoolean(JVMConfigurationKeys.DISABLE_STANDARD_SCRIPT_DEFINITION) &&
            StandardScriptDefinition !in configuration.getList(JVMConfigurationKeys.SCRIPT_DEFINITIONS)
        ) {
            configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, StandardScriptDefinition)
        }

        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) as? CliScriptDefinitionProvider
        if (scriptDefinitionProvider != null) {
            scriptDefinitionProvider.setScriptDefinitionsSources(configuration.getList(JVMConfigurationKeys.SCRIPT_DEFINITIONS_SOURCES))
            scriptDefinitionProvider.setScriptDefinitions(configuration.getList(JVMConfigurationKeys.SCRIPT_DEFINITIONS))

            // Register new file extensions
            val fileTypeRegistry = FileTypeRegistry.getInstance() as CoreFileTypeRegistry

            scriptDefinitionProvider.getKnownFilenameExtensions().filter {
                fileTypeRegistry.getFileTypeByExtension(it) != KotlinFileType.INSTANCE
            }.forEach {
                fileTypeRegistry.registerFileType(KotlinFileType.INSTANCE, it)
            }
        }

        sourceFiles += createKtFiles(project)
        sourceFiles.sortBy { it.virtualFile.path }

        if (scriptDefinitionProvider != null) {
            ScriptDependenciesProvider.getInstance(project).let { importsProvider ->
                configuration.addJvmClasspathRoots(
                    sourceFiles.mapNotNull(importsProvider::getScriptDependencies)
                        .flatMap { it.classpath }
                        .distinctBy { it.absolutePath })
            }
        }

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
        this.initialRoots = initialRoots

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
            configuration.getBoolean(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING)
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
            LightClassGenerationSupport.getInstance(project)
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

        for (packagePartProvider in packagePartProviders) {
            packagePartProvider.addRoots(newRoots, configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY))
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

    private fun createKtFiles(project: Project): List<KtFile> {
        val sourceRoots = getSourceRootsCheckingForDuplicates()

        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val psiManager = PsiManager.getInstance(project)

        val processedFiles = hashSetOf<VirtualFile>()
        val result = mutableListOf<KtFile>()

        val virtualFileCreator = PreprocessedFileCreator(project)

        for ((sourceRootPath, isCommon) in sourceRoots) {
            val vFile = localFileSystem.findFileByPath(sourceRootPath)
            if (vFile == null) {
                val message = "Source file or directory not found: $sourceRootPath"

                val buildFilePath = configuration.get(JVMConfigurationKeys.MODULE_XML_FILE)
                if (buildFilePath != null && Logger.isInitialized()) {
                    LOG.warn("$message\n\nbuild file path: $buildFilePath\ncontent:\n${buildFilePath.readText()}")
                }

                report(ERROR, message)
                continue
            }

            if (!vFile.isDirectory && vFile.fileType != KotlinFileType.INSTANCE) {
                report(ERROR, "Source entry is not a Kotlin file: $sourceRootPath")
                continue
            }

            for (file in File(sourceRootPath).walkTopDown()) {
                if (!file.isFile) continue

                val virtualFile = localFileSystem.findFileByPath(file.absolutePath)?.let(virtualFileCreator::create)
                if (virtualFile != null && processedFiles.add(virtualFile)) {
                    val psiFile = psiManager.findFile(virtualFile)
                    if (psiFile is KtFile) {
                        result.add(psiFile)
                        if (isCommon) {
                            psiFile.isCommonSource = true
                        }
                    }
                }
            }
        }

        return result
    }

    private fun report(severity: CompilerMessageSeverity, message: String) {
        configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(severity, message)
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinCoreEnvironment::class.java)

        private val APPLICATION_LOCK = Object()
        private var ourApplicationEnvironment: JavaCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        @JvmStatic
        fun createForProduction(
            parentDisposable: Disposable, configuration: CompilerConfiguration, configFiles: EnvironmentConfigFiles
        ): KotlinCoreEnvironment {
            val appEnv = getOrCreateApplicationEnvironmentForProduction(configuration)
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
            val environment = KotlinCoreEnvironment(parentDisposable, appEnv, configuration, configFiles)

            synchronized(APPLICATION_LOCK) {
                ourProjectCount++
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
            return KotlinCoreEnvironment(
                parentDisposable,
                createApplicationEnvironment(parentDisposable, configuration, unitTestMode = true),
                configuration,
                extensionConfigs
            )
        }

        // used in the daemon for jar cache cleanup
        val applicationEnvironment: JavaCoreApplicationEnvironment? get() = ourApplicationEnvironment

        private fun getOrCreateApplicationEnvironmentForProduction(configuration: CompilerConfiguration): JavaCoreApplicationEnvironment {
            synchronized(APPLICATION_LOCK) {
                if (ourApplicationEnvironment != null)
                    return ourApplicationEnvironment!!

                val parentDisposable = Disposer.newDisposable()
                ourApplicationEnvironment = createApplicationEnvironment(parentDisposable, configuration, unitTestMode = false)
                ourProjectCount = 0
                Disposer.register(parentDisposable, Disposable {
                    synchronized(APPLICATION_LOCK) {
                        ourApplicationEnvironment = null
                    }
                })
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
        ): JavaCoreApplicationEnvironment {
            Extensions.cleanRootArea(parentDisposable)
            registerAppExtensionPoints()
            val applicationEnvironment = object : JavaCoreApplicationEnvironment(parentDisposable, unitTestMode) {
                override fun createJrtFileSystem(): VirtualFileSystem? = CoreJrtFileSystem()
            }

            registerApplicationExtensionPointsAndExtensionsFrom(configuration, "extensions/compiler.xml")

            registerApplicationServicesForCLI(applicationEnvironment)
            registerApplicationServices(applicationEnvironment)

            return applicationEnvironment
        }

        private fun registerAppExtensionPoints() {
            val area = Extensions.getRootArea()

            CoreApplicationEnvironment.registerExtensionPoint(area, BinaryFileStubBuilders.EP_NAME, FileTypeExtensionPoint::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(area, FileContextProvider.EP_NAME, FileContextProvider::class.java)

            CoreApplicationEnvironment.registerExtensionPoint(area, MetaDataContributor.EP_NAME, MetaDataContributor::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(area, JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider::class.java)

            CoreApplicationEnvironment.registerExtensionPoint(area, ContainerProvider.EP_NAME, ContainerProvider::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(
                area, ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler::class.java
            )

            CoreApplicationEnvironment.registerExtensionPoint(area, TypeAnnotationModifier.EP_NAME, TypeAnnotationModifier::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(area, MetaLanguage.EP_NAME, MetaLanguage::class.java)

            IdeaExtensionPoints.registerVersionSpecificAppExtensionPoints(area)
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

            val pluginRoot =
                configuration.get(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT)?.let(::File)
                    ?: PathUtil.getResourcePathForClass(this::class.java).takeIf { it.hasConfigFile(configFilePath) }
                    // hack for load extensions when compiler run directly from project directory (e.g. in tests)
                    ?: File("idea/resources").takeIf { it.hasConfigFile(configFilePath) }
                    ?: throw IllegalStateException(
                        "Unable to find extension point configuration $configFilePath " +
                                "(cp:\n  ${(Thread.currentThread().contextClassLoader as? UrlClassLoader)?.urls?.joinToString("\n  ") { it.file }})"
                    )

            CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginRoot, configFilePath, Extensions.getRootArea())
        }

        private fun registerApplicationServicesForCLI(applicationEnvironment: JavaCoreApplicationEnvironment) {
            // ability to get text from annotations xml files
            applicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml")
            applicationEnvironment.registerParserDefinition(JavaParserDefinition())
        }

        // made public for Upsource
        @Suppress("MemberVisibilityCanBePrivate")
        @JvmStatic
        fun registerApplicationServices(applicationEnvironment: JavaCoreApplicationEnvironment) {
            with(applicationEnvironment) {
                registerFileType(KotlinFileType.INSTANCE, "kt")
                registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
                registerParserDefinition(KotlinParserDefinition())
                application.registerService(KotlinBinaryClassCache::class.java, KotlinBinaryClassCache())
                application.registerService(JavaClassSupers::class.java, JavaClassSupersImpl::class.java)
                application.registerService(TransactionGuard::class.java, TransactionGuardImpl::class.java)
            }
        }

        private fun registerProjectExtensionPoints(area: ExtensionsArea) {
            CoreApplicationEnvironment.registerExtensionPoint(
                area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor::class.java
            )
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder::class.java)

            IdeaExtensionPoints.registerVersionSpecificProjectExtensionPoints(area)
        }

        // made public for Upsource
        @JvmStatic
        fun registerProjectServices(projectEnvironment: JavaCoreProjectEnvironment, messageCollector: MessageCollector?) {
            with(projectEnvironment.project) {
                val scriptDefinitionProvider = CliScriptDefinitionProvider()
                registerService(ScriptDefinitionProvider::class.java, scriptDefinitionProvider)
                registerService(
                    ScriptDependenciesProvider::class.java,
                    CliScriptDependenciesProvider(projectEnvironment.project)
                )
                registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(this))
                registerService(KtLightClassForFacade.FacadeStubCache::class.java, KtLightClassForFacade.FacadeStubCache(this))
                registerService(ModuleAnnotationsResolver::class.java, CliModuleAnnotationsResolver())
                if (messageCollector != null) {
                    registerService(ScriptReportSink::class.java, CliScriptReportSink(messageCollector))
                }
            }
        }

        private fun registerProjectServicesForCLI(@Suppress("UNUSED_PARAMETER") projectEnvironment: JavaCoreProjectEnvironment) {
            /**
             * Note that Kapt may restart code analysis process, and CLI services should be aware of that.
             * Use PsiManager.getModificationTracker() to ensure that all the data you cached is still valid.
             */
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
                    val messageCollector = get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                    messageCollector?.report(ERROR, "No class roots are found in the JDK path: $javaRoot")
                } else {
                    addJvmSdkRoots(classesRoots)
                }
            }
        }
    }
}
