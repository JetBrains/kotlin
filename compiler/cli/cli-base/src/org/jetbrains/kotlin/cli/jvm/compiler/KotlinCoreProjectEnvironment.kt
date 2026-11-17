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

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.InferredAnnotationsManager
import com.intellij.core.CoreApplicationEnvironment.registerExtensionPoint
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.jvm.facade.JvmElementProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.PersistentFSConstants
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiJavaModuleModificationTracker
import com.intellij.psi.impl.PsiNameHelperImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.CliDiagnostics.INITIALIZATION_WARNING
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportInfo
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.useFastJarFileSystem
import org.jetbrains.kotlin.config.usePsiClassFilesReading
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaFixedElementSourceFactory
import org.jetbrains.kotlin.resolve.jvm.KotlinCliJavaFileManager
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

abstract class KotlinCoreProjectEnvironmentBase(
    disposable: Disposable,
    applicationEnvironment: KotlinCoreApplicationEnvironment
) : JavaCoreProjectEnvironment(disposable, applicationEnvironment) {
    override fun createCoreFileManager(): KotlinCliJavaFileManager = KotlinCliJavaFileManagerImpl(PsiManager.getInstance(project))
}

class KotlinCoreProjectEnvironment(
    disposable: Disposable,
    applicationEnvironment: KotlinCoreApplicationEnvironment,
    configuration: CompilerConfiguration
) : KotlinCoreProjectEnvironmentBase(disposable, applicationEnvironment) {

    val jarFileSystem: VirtualFileSystem

    init {
        setIdeaIoUseFallback()

        val useFastJarFS: Boolean = configuration.useFastJarFileSystem
        if (!useFastJarFS) {
            configuration.reportInfo("Using outdated version of JAR FS: it might make your build slower")
        }

        jarFileSystem = when {
            configuration.usePsiClassFilesReading -> applicationEnvironment.jarFileSystem
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

        registerExtensionsFromPlugins(configuration)
        configureProjectEnvironment(configuration)
    }

    private var extensionRegistered = false

    @OptIn(CoreEnvironmentDeprecation::class)
    override fun preregisterServices() {
        registerProjectExtensionPoints(project.extensionArea)
    }

    private fun registerExtensionsFromPlugins(configuration: CompilerConfiguration) {
        if (!extensionRegistered) {
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
            extensionRegistered = true
        }
    }

    @OptIn(K1Deprecation::class)
    private fun configureProjectEnvironment(configuration: CompilerConfiguration) {
        @Suppress("DEPRECATION")
        PersistentFSConstants::class.java.getDeclaredField("ourMaxIntellisenseFileSize")
            .apply { isAccessible = true }
            .setInt(null, FileUtilRt.LARGE_FOR_CONTENT_LOADING)

        registerProjectServices()

        for (extension in configuration.getCompilerExtensions(CompilerConfigurationExtension)) {
            extension.updateConfiguration(project, configuration)
        }
    }

    private fun registerProjectServices() {
        with(project) {
            registerService(JavaElementSourceFactory::class.java, JavaFixedElementSourceFactory::class.java)
            registerService(PsiJavaModuleModificationTracker::class.java, PsiJavaModuleModificationTracker::class.java)
            registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(this))
            if (getService(PsiNameHelper::class.java) == null) {
                registerService(PsiNameHelper::class.java, PsiNameHelperImpl::class.java)
            }
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

    companion object {
        @JvmStatic
        @CoreEnvironmentDeprecation
        fun registerProjectExtensionPoints(area: ExtensionsArea) {
            registerExtensionPoint(area, PsiTreeChangePreprocessor.EP.name, PsiTreeChangePreprocessor::class.java)
            registerExtensionPoint(area, PsiElementFinder.EP.name, PsiElementFinder::class.java)
            registerExtensionPoint(area, JvmElementProvider.EP_NAME, JvmElementProvider::class.java)
        }
    }
}
