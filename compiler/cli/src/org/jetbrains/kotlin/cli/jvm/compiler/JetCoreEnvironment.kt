/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.ContentBasedFileSubstitutor
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileContextProvider
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.impl.compiled.ClsStubBuilderFactory
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.util.containers.ContainerUtil
import kotlin.Function1
import kotlin.Unit
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.JavaElementFinder
import org.jetbrains.kotlin.asJava.KotlinLightClassForPackage
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.parsing.JetParserDefinition
import org.jetbrains.kotlin.parsing.JetScriptDefinitionProvider
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.utils.PathUtil

import java.io.File
import java.util.*

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING

SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
public class JetCoreEnvironment private(parentDisposable: Disposable, applicationEnvironment: JavaCoreApplicationEnvironment, configuration: CompilerConfiguration) {

    private val projectEnvironment: JavaCoreProjectEnvironment
    private val sourceFiles = ArrayList<JetFile>()
    private val classPath = ClassPath()

    private val annotationsManager: CoreExternalAnnotationsManager

    public val configuration: CompilerConfiguration

    {
        this.configuration = configuration.copy()
        this.configuration.setReadOnly(true)

        projectEnvironment = object : JavaCoreProjectEnvironment(parentDisposable, applicationEnvironment) {
            override fun preregisterServices() {
                registerProjectExtensionPoints(Extensions.getArea(getProject()))
            }
        }

        val project = projectEnvironment.getProject()
        annotationsManager = CoreExternalAnnotationsManager(project.getComponent<PsiManager>(javaClass<PsiManager>()))
        project.registerService<ExternalAnnotationsManager>(javaClass<ExternalAnnotationsManager>(), annotationsManager)
        project.registerService<DeclarationProviderFactoryService>(javaClass<DeclarationProviderFactoryService>(), CliDeclarationProviderFactoryService(sourceFiles))

        registerProjectServicesForCLI(projectEnvironment)
        registerProjectServices(projectEnvironment)

        for (path in configuration.getList<File>(JVMConfigurationKeys.CLASSPATH_KEY)) {
            addToClasspath(path)
        }
        for (path in configuration.getList<File>(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY)) {
            addExternalAnnotationsRoot(path)
        }
        sourceFiles.addAll(CompileEnvironmentUtil.getJetFiles(getProject(), getSourceRootsCheckingForDuplicates(), object : Function1<String, Unit> {
            override fun invoke(s: String): Unit {
                report(ERROR, s)
                return Unit.`INSTANCE$`
            }
        }))

        ContainerUtil.sort<JetFile>(sourceFiles, object : Comparator<JetFile> {
            override fun compare(o1: JetFile, o2: JetFile): Int {
                return o1.getVirtualFile().getPath().compareToIgnoreCase(o2.getVirtualFile().getPath())
            }
        })

        JetScriptDefinitionProvider.getInstance(project).addScriptDefinitions(configuration.getList<JetScriptDefinition>(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY))

        project.registerService<VirtualFileFinderFactory>(javaClass<VirtualFileFinderFactory>(), CliVirtualFileFinderFactory(classPath))

        ExternalDeclarationsProvider.registerExtensionPoint(project)
        ExpressionCodegenExtension.registerExtensionPoint(project)

        for (registrar in configuration.getList<ComponentRegistrar>(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)) {
            registrar.registerProjectComponents(project, configuration)
        }
    }

    private fun getMyApplicationEnvironment(): CoreApplicationEnvironment {
        return projectEnvironment.getEnvironment()
    }

    public fun getApplication(): MockApplication {
        return getMyApplicationEnvironment().getApplication()
    }

    public fun getProject(): Project {
        return projectEnvironment.getProject()
    }

    private fun addExternalAnnotationsRoot(path: File) {
        if (!path.exists()) {
            report(WARNING, "Annotations path entry points to a non-existent location: " + path)
            return
        }
        annotationsManager.addExternalAnnotationsRoot(PathUtil.jarFileOrDirectoryToVirtualFile(path))
    }

    private fun addToClasspath(path: File) {
        if (path.isFile()) {
            val jarFile = getMyApplicationEnvironment().getJarFileSystem().findFileByPath(path + "!/")
            if (jarFile == null) {
                report(WARNING, "Classpath entry points to a file that is not a JAR archive: " + path)
                return
            }
            projectEnvironment.addJarToClassPath(path)
            classPath.add(jarFile)
        }
        else {
            val root = getMyApplicationEnvironment().getLocalFileSystem().findFileByPath(path.getAbsolutePath())
            if (root == null) {
                report(WARNING, "Classpath entry points to a non-existent location: " + path)
                return
            }
            projectEnvironment.addSourcesToClasspath(root)
            classPath.add(root)
        }
    }

    private fun getSourceRootsCheckingForDuplicates(): Collection<String> {
        val uniqueSourceRoots = Sets.newLinkedHashSet<String>()

        for (sourceRoot in configuration.getList<String>(CommonConfigurationKeys.SOURCE_ROOTS_KEY)) {
            if (!uniqueSourceRoots.add(sourceRoot)) {
                report(WARNING, "Duplicate source root: " + sourceRoot)
            }
        }

        return uniqueSourceRoots
    }

    public fun getSourceFiles(): List<JetFile> {
        return sourceFiles
    }

    private fun report(severity: CompilerMessageSeverity, message: String) {
        val messageCollector = configuration.get<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        if (messageCollector != null) {
            messageCollector.report(severity, message, CompilerMessageLocation.NO_LOCATION)
        }
        else {
            throw CompileEnvironmentException(message)
        }
    }

    companion object {

        private val APPLICATION_LOCK = Object()
        private var ourApplicationEnvironment: JavaCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        public fun createForProduction(parentDisposable: Disposable, configuration: CompilerConfiguration, configFilePaths: List<String>): JetCoreEnvironment {
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
            val environment = JetCoreEnvironment(parentDisposable, getOrCreateApplicationEnvironmentForProduction(configuration, configFilePaths), configuration)

            synchronized (APPLICATION_LOCK) {
                ourProjectCount++
            }
            return environment
        }

        TestOnly
        public fun createForTests(parentDisposable: Disposable, configuration: CompilerConfiguration, extensionConfigs: List<String>): JetCoreEnvironment {
            // Tests are supposed to create a single project and dispose it right after use
            return JetCoreEnvironment(parentDisposable, createApplicationEnvironment(parentDisposable, configuration, extensionConfigs), configuration)
        }

        private fun getOrCreateApplicationEnvironmentForProduction(configuration: CompilerConfiguration, configFilePaths: List<String>): JavaCoreApplicationEnvironment {
            synchronized (APPLICATION_LOCK) {
                if (ourApplicationEnvironment != null) return ourApplicationEnvironment

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
                return ourApplicationEnvironment
            }
        }

        public fun disposeApplicationEnvironment() {
            synchronized (APPLICATION_LOCK) {
                if (ourApplicationEnvironment == null) return
                val environment = ourApplicationEnvironment
                ourApplicationEnvironment = null
                Disposer.dispose(environment.getParentDisposable())
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
            CoreApplicationEnvironment.registerExtensionPoint<ContentBasedFileSubstitutor>(Extensions.getRootArea(), ContentBasedFileSubstitutor.EP_NAME, javaClass<ContentBasedFileSubstitutor>())
            CoreApplicationEnvironment.registerExtensionPoint<FileTypeExtensionPoint<Any>>(Extensions.getRootArea(), BinaryFileStubBuilders.EP_NAME, javaClass<FileTypeExtensionPoint<Any>>())
            CoreApplicationEnvironment.registerExtensionPoint<FileContextProvider>(Extensions.getRootArea(), FileContextProvider.EP_NAME, javaClass<FileContextProvider>())
            //
            CoreApplicationEnvironment.registerExtensionPoint<MetaDataContributor>(Extensions.getRootArea(), MetaDataContributor.EP_NAME, javaClass<MetaDataContributor>())
            CoreApplicationEnvironment.registerExtensionPoint<ClsStubBuilderFactory>(Extensions.getRootArea(), ClsStubBuilderFactory.EP_NAME, javaClass<ClsStubBuilderFactory<PsiFile>>())
            CoreApplicationEnvironment.registerExtensionPoint<PsiAugmentProvider>(Extensions.getRootArea(), PsiAugmentProvider.EP_NAME, javaClass<PsiAugmentProvider>())
            CoreApplicationEnvironment.registerExtensionPoint<JavaMainMethodProvider>(Extensions.getRootArea(), JavaMainMethodProvider.EP_NAME, javaClass<JavaMainMethodProvider>())
            //
            CoreApplicationEnvironment.registerExtensionPoint<ContainerProvider>(Extensions.getRootArea(), ContainerProvider.EP_NAME, javaClass<ContainerProvider>())
            CoreApplicationEnvironment.registerExtensionPoint<ClsCustomNavigationPolicy>(Extensions.getRootArea(), ClsCustomNavigationPolicy.EP_NAME, javaClass<ClsCustomNavigationPolicy>())
            CoreApplicationEnvironment.registerExtensionPoint<Decompiler>(Extensions.getRootArea(), ClassFileDecompilers.EP_NAME, javaClass<ClassFileDecompilers.Decompiler>())
        }

        private fun registerApplicationExtensionPointsAndExtensionsFrom(configuration: CompilerConfiguration, configFilePath: String) {
            val locator = configuration.get<CompilerJarLocator>(JVMConfigurationKeys.COMPILER_JAR_LOCATOR)
            var pluginRoot = if (locator == null) PathUtil.getPathUtilJar() else locator.getCompilerJar()

            val app = ApplicationManager.getApplication()
            val parentFile = pluginRoot.getParentFile()

            if (pluginRoot.isDirectory() && app != null && app.isUnitTestMode() && FileUtil.toCanonicalPath(parentFile.getPath()).endsWith("out/production")) {
                // hack for load extensions when compiler run directly from out directory(e.g. in tests)
                val srcDir = parentFile.getParentFile().getParentFile()
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
        public fun registerApplicationServices(applicationEnvironment: JavaCoreApplicationEnvironment) {
            applicationEnvironment.registerFileType(JetFileType.INSTANCE, "kt")
            applicationEnvironment.registerFileType(JetFileType.INSTANCE, "ktm")
            applicationEnvironment.registerFileType(JetFileType.INSTANCE, JetParserDefinition.STD_SCRIPT_SUFFIX) // should be renamed to kts
            applicationEnvironment.registerParserDefinition(JetParserDefinition())

            applicationEnvironment.getApplication().registerService<KotlinBinaryClassCache>(javaClass<KotlinBinaryClassCache>(), KotlinBinaryClassCache())
        }

        private fun registerProjectExtensionPoints(area: ExtensionsArea) {
            CoreApplicationEnvironment.registerExtensionPoint<PsiTreeChangePreprocessor>(area, PsiTreeChangePreprocessor.EP_NAME, javaClass<PsiTreeChangePreprocessor>())
            CoreApplicationEnvironment.registerExtensionPoint<PsiElementFinder>(area, PsiElementFinder.EP_NAME, javaClass<PsiElementFinder>())
        }

        // made public for Upsource
        public fun registerProjectServices(projectEnvironment: JavaCoreProjectEnvironment) {
            val project = projectEnvironment.getProject()
            project.registerService<JetScriptDefinitionProvider>(javaClass<JetScriptDefinitionProvider>(), JetScriptDefinitionProvider())

            project.registerService<KotlinJavaPsiFacade>(javaClass<KotlinJavaPsiFacade>(), KotlinJavaPsiFacade(project))
            project.registerService<KotlinLightClassForPackage.FileStubCache>(javaClass<KotlinLightClassForPackage.FileStubCache>(), KotlinLightClassForPackage.FileStubCache(project))
        }

        private fun registerProjectServicesForCLI(projectEnvironment: JavaCoreProjectEnvironment) {
            val project = projectEnvironment.getProject()
            project.registerService<CoreJavaFileManager>(javaClass<CoreJavaFileManager>(), ServiceManager.getService<JavaFileManager>(project, javaClass<JavaFileManager>()) as CoreJavaFileManager)
            val cliLightClassGenerationSupport = CliLightClassGenerationSupport(project)
            project.registerService<LightClassGenerationSupport>(javaClass<LightClassGenerationSupport>(), cliLightClassGenerationSupport)
            project.registerService<CliLightClassGenerationSupport>(javaClass<CliLightClassGenerationSupport>(), cliLightClassGenerationSupport)
            project.registerService<CodeAnalyzerInitializer>(javaClass<CodeAnalyzerInitializer>(), cliLightClassGenerationSupport)

            val area = Extensions.getArea(project)

            area.getExtensionPoint<PsiElementFinder>(PsiElementFinder.EP_NAME).registerExtension(PsiElementFinderImpl(project, ServiceManager.getService<JavaFileManager>(project, javaClass<JavaFileManager>())))

            area.getExtensionPoint<PsiElementFinder>(PsiElementFinder.EP_NAME).registerExtension(JavaElementFinder(project, cliLightClassGenerationSupport))
        }
    }
}
