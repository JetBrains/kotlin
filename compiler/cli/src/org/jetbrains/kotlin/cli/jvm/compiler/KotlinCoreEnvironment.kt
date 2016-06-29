/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.google.common.collect.Sets
import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.PersistentFSConstants
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.psi.FileContextProvider
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.psi.util.JavaClassSupers
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.kotlinSourceRoots
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.lang.IllegalStateException
import java.util.*

class KotlinCoreEnvironment private constructor(
        parentDisposable: Disposable, 
        applicationEnvironment: JavaCoreApplicationEnvironment, 
        configuration: CompilerConfiguration
) {

    private val projectEnvironment: JavaCoreProjectEnvironment = object : KotlinCoreProjectEnvironment(parentDisposable, applicationEnvironment) {
        override fun preregisterServices() {
            registerProjectExtensionPoints(Extensions.getArea(getProject()))
        }
    }
    private val sourceFiles = ArrayList<KtFile>()
    private val javaRoots = ArrayList<JavaRoot>()

    val configuration: CompilerConfiguration = configuration.copy().apply { setReadOnly(true) }

    init {
        PersistentFSConstants.setMaxIntellisenseFileSize(FileUtilRt.LARGE_FOR_CONTENT_LOADING)
    }

    init {
        val project = projectEnvironment.project
        project.registerService(DeclarationProviderFactoryService::class.java, CliDeclarationProviderFactoryService(sourceFiles))
        project.registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl())

        registerProjectServicesForCLI(projectEnvironment)
        registerProjectServices(projectEnvironment)

        fillClasspath(configuration)
        val index = initJvmDependenciesIndex()

        sourceFiles.addAll(CompileEnvironmentUtil.getKtFiles(project, getSourceRootsCheckingForDuplicates(), this.configuration, {
            message ->
            report(ERROR, message)
        }))
        sourceFiles.sortedWith(object : Comparator<KtFile> {
            override fun compare(o1: KtFile, o2: KtFile): Int {
                return o1.virtualFile.path.compareTo(o2.virtualFile.path, ignoreCase = true)
            }
        })

        KotlinScriptDefinitionProvider.getInstance(project).setScriptDefinitions(
                configuration.getList(JVMConfigurationKeys.SCRIPT_DEFINITIONS)
        )

        project.registerService(JvmVirtualFileFinderFactory::class.java, JvmCliVirtualFileFinderFactory(index))

        ExternalDeclarationsProvider.registerExtensionPoint(project)
        ExpressionCodegenExtension.registerExtensionPoint(project)
        ClassBuilderInterceptorExtension.registerExtensionPoint(project)
        AnalysisCompletedHandlerExtension.registerExtensionPoint(project)
        PackageFragmentProviderExtension.registerExtensionPoint(project)
        StorageComponentContainerContributor.registerExtensionPoint(project)

        for (registrar in configuration.getList(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)) {
            registrar.registerProjectComponents(project, configuration)
        }
    }

    private val applicationEnvironment: CoreApplicationEnvironment
        get() = projectEnvironment.environment

    val application: MockApplication
        get() = applicationEnvironment.application

    val project: Project
        get() = projectEnvironment.project

    val sourceLinesOfCode: Int by lazy { countLinesOfCode(sourceFiles) }

    private fun initJvmDependenciesIndex(): JvmDependenciesIndex {
        val index = JvmDependenciesIndex(javaRoots)
        val fileManager = ServiceManager.getService(project, CoreJavaFileManager::class.java)
        (fileManager as KotlinCliJavaFileManagerImpl).initIndex(index)
        return index
    }

    val appendJavaSourceRootsHandler = fun(roots: List<File>) {
        addJavaSourceRoots(roots.map { JavaSourceRoot(it, null) })
    }

    init {
        project.putUserData(APPEND_JAVA_SOURCE_ROOTS_HANDLER_KEY, appendJavaSourceRootsHandler)
    }
    
    fun addJavaSourceRoots(newRoots: List<JavaSourceRoot>) {
        newRoots.forEach { addJavaRoot(it) }
        initJvmDependenciesIndex()
    }

    fun countLinesOfCode(sourceFiles: List<KtFile>): Int  =
            sourceFiles.sumBy {
                val text = it.text
                StringUtil.getLineBreakCount(it.text) + (if (StringUtil.endsWithLineBreak(text)) 0 else 1)
            }
    
    private fun fillClasspath(configuration: CompilerConfiguration) {
        for (root in configuration.getList(JVMConfigurationKeys.CONTENT_ROOTS)) {
            val javaRoot = root as? JvmContentRoot ?: continue
            addJavaRoot(javaRoot)
        }
    }

    private fun addJavaRoot(javaRoot: JvmContentRoot) {
        val virtualFile = contentRootToVirtualFile(javaRoot) ?: return

        projectEnvironment.addSourcesToClasspath(virtualFile)

        val prefixPackageFqName = (javaRoot as? JavaSourceRoot)?.packagePrefix?.let {
            if (isValidJavaFqName(it)) {
                FqName(it)
            }
            else {
                report(WARNING, "Invalid package prefix name is ignored: $it")
                null
            }
        }

        val rootType = when (javaRoot) {
            is JavaSourceRoot -> JavaRoot.RootType.SOURCE
            is JvmClasspathRoot -> JavaRoot.RootType.BINARY
            else -> throw IllegalStateException()
        }

        javaRoots.add(JavaRoot(virtualFile, rootType, prefixPackageFqName))
    }

    fun contentRootToVirtualFile(root: JvmContentRoot): VirtualFile? {
        when (root) {
            is JvmClasspathRoot -> {
                return if (root.file.isFile) findJarRoot(root) else findLocalDirectory(root)
            }
            is JavaSourceRoot -> {
                return if (root.file.isDirectory) findLocalDirectory(root) else null
            }
            else -> throw IllegalStateException("Unexpected root: $root")
        }
    }

    private fun findLocalDirectory(root: JvmContentRoot): VirtualFile? {
        val path = root.file
        val localFile = applicationEnvironment.localFileSystem.findFileByPath(path.absolutePath)
        if (localFile == null) {
            report(WARNING, "Classpath entry points to a non-existent location: $path")
            return null
        }
        return localFile
    }

    private fun findJarRoot(root: JvmClasspathRoot): VirtualFile? {
        val path = root.file
        val jarFile = applicationEnvironment.jarFileSystem.findFileByPath("${path}!/")
        if (jarFile == null) {
            report(WARNING, "Classpath entry points to a file that is not a JAR archive: $path")
            return null
        }
        return jarFile
    }

    private fun getSourceRootsCheckingForDuplicates(): Collection<String> {
        val uniqueSourceRoots = Sets.newLinkedHashSet<String>()

        configuration.kotlinSourceRoots.forEach { path ->
            if (!uniqueSourceRoots.add(path)) {
                report(WARNING, "Duplicate source root: $path")
            }
        }

        return uniqueSourceRoots
    }

    fun getSourceFiles(): List<KtFile> = sourceFiles

    private fun report(severity: CompilerMessageSeverity, message: String) {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        messageCollector.report(severity, message, CompilerMessageLocation.NO_LOCATION)
    }

    companion object {
        init {
            System.getProperties().setProperty("idea.plugins.compatible.build", "162.9999")
        }

        private val APPLICATION_LOCK = Object()
        private var ourApplicationEnvironment: JavaCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        @JvmStatic fun createForProduction(
                parentDisposable: Disposable, configuration: CompilerConfiguration, configFilePaths: List<String>
        ): KotlinCoreEnvironment {
            val appEnv = getOrCreateApplicationEnvironmentForProduction(configuration, configFilePaths)
            // Disposing of the environment is unsafe in production then parallel builds are enabled, but turning it off universally
            // breaks a lot of tests, therefore it is disabled for production and enabled for tests
            if (!(System.getProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY).toBooleanLenient() ?: false)) {
                // JPS may run many instances of the compiler in parallel (there's an option for compiling independent modules in parallel in IntelliJ)
                // All projects share the same ApplicationEnvironment, and when the last project is disposed, the ApplicationEnvironment is disposed as well
                Disposer.register(parentDisposable, object : Disposable {
                    override fun dispose() {
                        synchronized (APPLICATION_LOCK) {
                            if (--ourProjectCount <= 0) {
                                disposeApplicationEnvironment()
                            }
                        }
                    }
                })
            }
            val environment = KotlinCoreEnvironment(parentDisposable, appEnv, configuration)

            synchronized (APPLICATION_LOCK) {
                ourProjectCount++
            }
            return environment
        }

        @TestOnly
        @JvmStatic fun createForTests(
                parentDisposable: Disposable, configuration: CompilerConfiguration, extensionConfigs: List<String>
        ): KotlinCoreEnvironment {
            // Tests are supposed to create a single project and dispose it right after use
            return KotlinCoreEnvironment(parentDisposable, createApplicationEnvironment(parentDisposable, configuration, extensionConfigs), configuration)
        }

        // used in the daemon for jar cache cleanup
        val applicationEnvironment: JavaCoreApplicationEnvironment? get() = ourApplicationEnvironment

        private fun getOrCreateApplicationEnvironmentForProduction(configuration: CompilerConfiguration, configFilePaths: List<String>): JavaCoreApplicationEnvironment {
            synchronized (APPLICATION_LOCK) {
                if (ourApplicationEnvironment != null)
                    return ourApplicationEnvironment!!

                val parentDisposable = Disposer.newDisposable()
                ourApplicationEnvironment = createApplicationEnvironment(parentDisposable, configuration, configFilePaths)
                ourProjectCount = 0
                Disposer.register(parentDisposable, object : Disposable {
                    override fun dispose() {
                        synchronized (APPLICATION_LOCK) {
                            ourApplicationEnvironment = null
                        }
                    }
                })
                return ourApplicationEnvironment!!
            }
        }

        fun disposeApplicationEnvironment() {
            synchronized (APPLICATION_LOCK) {
                val environment = ourApplicationEnvironment ?: return
                ourApplicationEnvironment = null
                Disposer.dispose(environment.parentDisposable)
                ZipHandler.clearFileAccessorCache()
            }
        }

        private fun createApplicationEnvironment(parentDisposable: Disposable, configuration: CompilerConfiguration, configFilePaths: List<String>): JavaCoreApplicationEnvironment {
            Extensions.cleanRootArea(parentDisposable)
            registerAppExtensionPoints()
            val applicationEnvironment = JavaCoreApplicationEnvironment(parentDisposable)

            for (configPath in configFilePaths) {
                registerApplicationExtensionPointsAndExtensionsFrom(configuration, configPath)
            }

            registerApplicationServicesForCLI(applicationEnvironment)
            registerApplicationServices(applicationEnvironment)

            return applicationEnvironment
        }

        private fun registerAppExtensionPoints() {
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), BinaryFileStubBuilders.EP_NAME, FileTypeExtensionPoint::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), FileContextProvider.EP_NAME, FileContextProvider::class.java)
            //
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), MetaDataContributor.EP_NAME, MetaDataContributor::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider::class.java)
            //
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ContainerProvider.EP_NAME, ContainerProvider::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClsCustomNavigationPolicy.EP_NAME, ClsCustomNavigationPolicy::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler::class.java)
        }

        private fun registerApplicationExtensionPointsAndExtensionsFrom(configuration: CompilerConfiguration, configFilePath: String) {
            val locator = configuration.get(CLIConfigurationKeys.COMPILER_JAR_LOCATOR)
            var pluginRoot = if (locator == null) PathUtil.getPathUtilJar() else locator.compilerJar

            val app = ApplicationManager.getApplication()
            val parentFile = pluginRoot.parentFile

            if (pluginRoot.isDirectory && app != null && app.isUnitTestMode
                && FileUtil.toCanonicalPath(parentFile.path).endsWith("out/production")) {
                // hack for load extensions when compiler run directly from out directory(e.g. in tests)
                val srcDir = parentFile.parentFile.parentFile
                pluginRoot = File(srcDir, "idea/src")
            }

            CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginRoot, configFilePath, Extensions.getRootArea())
        }

        private fun registerApplicationServicesForCLI(applicationEnvironment: JavaCoreApplicationEnvironment) {
            // ability to get text from annotations xml files
            applicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml")
            applicationEnvironment.registerParserDefinition(JavaParserDefinition())
        }

        // made public for Upsource
        @JvmStatic fun registerApplicationServices(applicationEnvironment: JavaCoreApplicationEnvironment) {
            with(applicationEnvironment) {
                registerFileType(KotlinFileType.INSTANCE, "kt")
                registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
                registerParserDefinition(KotlinParserDefinition())
                application.registerService(KotlinBinaryClassCache::class.java, KotlinBinaryClassCache())
                application.registerService(JavaClassSupers::class.java, JavaClassSupersImpl::class.java)
            }
        }

        private fun registerProjectExtensionPoints(area: ExtensionsArea) {
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder::class.java)
        }

        // made public for Upsource
        @JvmStatic fun registerProjectServices(projectEnvironment: JavaCoreProjectEnvironment) {
            with (projectEnvironment.project) {
                registerService(KotlinScriptDefinitionProvider::class.java, KotlinScriptDefinitionProvider())
                registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(this))
                registerService(KtLightClassForFacade.FacadeStubCache::class.java, KtLightClassForFacade.FacadeStubCache(this))
            }
        }

        private fun registerProjectServicesForCLI(projectEnvironment: JavaCoreProjectEnvironment) {
            /**
             * Note that Kapt may restart code analysis process, and CLI services should be aware of that.
             * Use PsiManager.getModificationTracker() to ensure that all the data you cached is still valid.
             */

            with (projectEnvironment.project) {
                registerService(CoreJavaFileManager::class.java, ServiceManager.getService(this, JavaFileManager::class.java) as CoreJavaFileManager)

                val cliLightClassGenerationSupport = CliLightClassGenerationSupport(this)
                registerService(LightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                registerService(CliLightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
                registerService(CodeAnalyzerInitializer::class.java, cliLightClassGenerationSupport)

                registerService(ExternalAnnotationsManager::class.java, MockExternalAnnotationsManager())
                registerService(InferredAnnotationsManager::class.java, MockInferredAnnotationsManager())

                val area = Extensions.getArea(this)

                area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(JavaElementFinder(this, cliLightClassGenerationSupport))
                area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(
                        PsiElementFinderImpl(this, ServiceManager.getService(this, JavaFileManager::class.java)))
            }
        }
    }
}
