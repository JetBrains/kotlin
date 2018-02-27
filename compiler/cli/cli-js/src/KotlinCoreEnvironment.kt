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

/// COPIED
package org.jetbrains.kotlin.cli.js

import com.intellij.codeInsight.ContainerProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.MetaLanguage
import com.intellij.lang.jvm.facade.JvmElementProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.PersistentFSConstants
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.extensions.PreprocessedVirtualFileFactoryExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ModuleAnnotationsResolver
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*

class KotlinCoreEnvironment private constructor(
    parentDisposable: Disposable,
    applicationEnvironment: JavaCoreApplicationEnvironment,
    configuration: CompilerConfiguration
) {

    private val projectEnvironment: JavaCoreProjectEnvironment =
        object : JavaCoreProjectEnvironment(parentDisposable, applicationEnvironment) {
            override fun preregisterServices() {
                registerProjectExtensionPoints(Extensions.getArea(project))
            }
        }
    private val sourceFiles = ArrayList<KtFile>()

    val configuration: CompilerConfiguration = configuration.copy()

    init {
        PersistentFSConstants.setMaxIntellisenseFileSize(FileUtilRt.LARGE_FOR_CONTENT_LOADING)
    }

    init {
        val project = projectEnvironment.project

        SyntheticResolveExtension.registerExtensionPoint(project)
        StorageComponentContainerContributor.registerExtensionPoint(project)
        DeclarationAttributeAltererExtension.registerExtensionPoint(project)
        PreprocessedVirtualFileFactoryExtension.registerExtensionPoint(project)

        project.registerService(DeclarationProviderFactoryService::class.java, CliDeclarationProviderFactoryService(sourceFiles))

        registerProjectServices(projectEnvironment)
    }

    val project: Project
        get() = projectEnvironment.project

    fun getSourceFiles(): List<KtFile> = sourceFiles

    fun addSourceFiles(files: List<KtFile>) {
        sourceFiles.addAll(files)
    }

    companion object {
        init {
            setCompatibleBuild()
        }

        private val APPLICATION_LOCK = Object()
        private var ourApplicationEnvironment: JavaCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        fun createForProduction(
            parentDisposable: Disposable, configuration: CompilerConfiguration, configFilePaths: List<String>
        ): KotlinCoreEnvironment {
            setCompatibleBuild()
            val appEnv = getOrCreateApplicationEnvironmentForProduction(configuration, configFilePaths)

            Disposer.register(parentDisposable, Disposable {
                synchronized(APPLICATION_LOCK) {
                    if (--ourProjectCount <= 0) {
                        disposeApplicationEnvironment()
                    }
                }
            })

            val environment = KotlinCoreEnvironment(parentDisposable, appEnv, configuration)

            synchronized(APPLICATION_LOCK) {
                ourProjectCount++
            }
            return environment
        }

        @JvmStatic
        private fun setCompatibleBuild() {
            System.getProperties().setProperty("idea.plugins.compatible.build", "171.9999")
        }

        private fun getOrCreateApplicationEnvironmentForProduction(
            configuration: CompilerConfiguration,
            configFilePaths: List<String>
        ): JavaCoreApplicationEnvironment {
            synchronized(APPLICATION_LOCK) {
                if (ourApplicationEnvironment != null)
                    return ourApplicationEnvironment!!

                val parentDisposable = Disposer.newDisposable()
                ourApplicationEnvironment = createApplicationEnvironment(parentDisposable, configuration, configFilePaths)
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
            parentDisposable: Disposable,
            configuration: CompilerConfiguration,
            configFilePaths: List<String>
        ): JavaCoreApplicationEnvironment {
            Extensions.cleanRootArea(parentDisposable)
            registerAppExtensionPoints()
            val applicationEnvironment = JavaCoreApplicationEnvironment(parentDisposable)

            for (configPath in configFilePaths) {
                registerApplicationExtensionPointsAndExtensionsFrom(configuration, configPath)
            }

            registerApplicationServices(applicationEnvironment)

            return applicationEnvironment
        }

        private fun registerAppExtensionPoints() {
            val area = Extensions.getRootArea()
            CoreApplicationEnvironment.registerExtensionPoint(area, ContainerProvider.EP_NAME, ContainerProvider::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), MetaLanguage.EP_NAME, MetaLanguage::class.java)
        }

        private fun registerApplicationExtensionPointsAndExtensionsFrom(configuration: CompilerConfiguration, configFilePath: String) {
            var pluginRoot = PathUtil.pathUtilJar

            val app = ApplicationManager.getApplication()
            val parentFile = pluginRoot.parentFile

            if (pluginRoot.isDirectory && app != null && app.isUnitTestMode
                && FileUtil.toCanonicalPath(parentFile.path).endsWith("out/production")
            ) {
                // hack for load extensions when compiler run directly from out directory(e.g. in tests)
                val srcDir = parentFile.parentFile.parentFile
                pluginRoot = File(srcDir, "idea/src")
            }

            CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginRoot, configFilePath, Extensions.getRootArea())
        }

        private fun registerApplicationServices(applicationEnvironment: JavaCoreApplicationEnvironment) {
            with(applicationEnvironment) {
                registerFileType(KotlinFileType.INSTANCE, "kt")
                registerParserDefinition(KotlinParserDefinition())
            }
        }

        private fun registerProjectExtensionPoints(area: ExtensionsArea) {
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, PsiElementFinder::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(area, JvmElementProvider.EP_NAME, JvmElementProvider::class.java)
        }

        private fun registerProjectServices(projectEnvironment: JavaCoreProjectEnvironment) {
            projectEnvironment.project.registerService(ModuleAnnotationsResolver::class.java, DummyModuleAnnotationsResolver())
        }
    }
}

private class DummyModuleAnnotationsResolver : ModuleAnnotationsResolver {
    override fun getAnnotationsOnContainingModule(descriptor: DeclarationDescriptor) = emptyList<ClassId>()
}
