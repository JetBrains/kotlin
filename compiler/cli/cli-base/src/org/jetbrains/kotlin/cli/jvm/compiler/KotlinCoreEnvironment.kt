/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DEPRECATION_ERROR", "UnstableApiUsage")

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.impl.*
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.CliDiagnostics.ROOTS_RESOLUTION_WARNING
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.index.*
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleResolver
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.io.File

@OptIn(CoreEnvironmentDeprecation::class)
class KotlinCoreEnvironment private constructor(
    val projectEnvironment: KotlinCoreProjectEnvironment,
    val configuration: CompilerConfiguration
) {
    private val rootsIndex: JvmDependenciesDynamicCompoundIndex

    val packagePartProviders: List<JvmPackagePartProvider>
        field = mutableListOf<JvmPackagePartProvider>()

    private val classpathRootsResolver: ClasspathRootsResolver
    private val initialRoots = ArrayList<JavaRoot>()

    init {
        val project = projectEnvironment.project

        val javaFileManager = project.getService(CoreJavaFileManager::class.java) as KotlinCliJavaFileManagerImpl

        val jdkHome = configuration.jdkHome
        val releaseTarget = configuration.jdkRelease
        val javaModuleFinder = CliJavaModuleFinder(jdkHome, configuration, javaFileManager, releaseTarget)

        val outputDirectory = configuration.modules.singleOrNull()?.getOutputDirectory() ?: configuration.outputDirectory?.absolutePath
        val contentRoots = configuration.contentRoots

        classpathRootsResolver = ClasspathRootsResolver(
            configuration,
            additionalModules = configuration.additionalJavaModules,
            this::contentRootToVirtualFile,
            javaModuleFinder,
            requireStdlibModule = !configuration.allowKotlinPackage,
            outputDirectory?.let(this::findLocalFile),
            javaFileManager::findClassOnClasspath,
            jdkRelease = releaseTarget,
            hasKotlinSources = contentRoots.any { it is KotlinSourceRoot },
        )

        (val initialRoots = roots, val javaModules = modules) = classpathRootsResolver.convertClasspathRoots(contentRoots)
        this.initialRoots.addAll(initialRoots)

        val [roots, singleJavaFileRoots] = initialRoots.partition { (file) ->
            file.isDirectory || file.extension != JavaFileType.DEFAULT_EXTENSION
        }

        // REPL updates classpath dynamically
        rootsIndex = JvmDependenciesDynamicCompoundIndex(shouldOnlyFindFirstClass = true).apply {
            addIndex(JvmDependenciesIndexImpl(roots))
            updateClasspathFromRootsIndex(this)
        }

        val perfManager = configuration.perfManager

        javaFileManager.initialize(
            rootsIndex,
            packagePartProviders,
            SingleJavaFileRootsIndex(singleJavaFileRoots),
            configuration.usePsiClassFilesReading,
            javaModuleFinder,
            perfManager,
        )

        val fileFinderFactory = CliVirtualFileFinderFactory(rootsIndex, enableSearchInCtSym = releaseTarget != null, perfManager)
        project.registerService(
            JavaModuleResolver::class.java,
            CliJavaModuleResolver(
                classpathRootsResolver.javaModuleGraph, javaModules, javaModuleFinder.systemModules.toList(),
                fileFinderFactory.create(JvmClasspath.ProjectLibraries()),
            )
        )

        project.registerService(VirtualFileFinderFactory::class.java, fileFinderFactory)
        project.registerService(MetadataFinderFactory::class.java, CliMetadataFinderFactory(fileFinderFactory))
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
        get() = rootsIndex.indexedRoots.map { it.file }.toList()

    private fun updateClasspathFromRootsIndex(index: JvmDependenciesIndex) {
        index.indexedRoots.forEach {
            projectEnvironment.addSourcesToClasspath(it.file)
        }
    }

    fun updateClasspath(contentRoots: List<ContentRoot>): List<File>? {
        // TODO: add new Java modules to CliJavaModuleResolver
        val newRoots = classpathRootsResolver.convertClasspathRoots(contentRoots).roots - initialRoots

        val unindexedRoots = rootsIndex.getUnindexedRoots(newRoots)
        if (unindexedRoots.isEmpty()) return null

        val newIndex = JvmDependenciesIndexImpl(unindexedRoots)
        rootsIndex.addIndex(newIndex)
        updateClasspathFromRootsIndex(newIndex)

        if (packagePartProviders.isEmpty()) {
            initialRoots.addAll(newRoots)
        } else {
            for (packagePartProvider in packagePartProviders) {
                packagePartProvider.addRoots(newRoots, configuration)
            }
        }

        configuration.addAll(CLIConfigurationKeys.CONTENT_ROOTS, contentRoots - configuration.contentRoots)

        return newIndex.indexedRoots.map { (file) ->
            VfsUtilCore.virtualToIoFile(VfsUtilCore.getVirtualFileForJar(file) ?: file)
        }.toList()
    }

    private fun contentRootToVirtualFile(root: JvmContentRootBase): VirtualFile? {
        return when (root) {
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
    }

    private fun findLocalFile(path: String): VirtualFile? {
        return applicationEnvironment.localFileSystem.findFileByPath(path)
    }

    private fun findExistingRoot(root: JvmContentRoot, rootDescription: String): VirtualFile? {
        return findLocalFile(root.file.absolutePath).also {
            if (it == null) {
                configuration.report(ROOTS_RESOLUTION_WARNING, "$rootDescription points to a non-existent location: ${root.file}")
            }
        }
    }

    private fun findJarRoot(file: File): VirtualFile? {
        return projectEnvironment.jarFileSystem.findFileByPath("$file${URLUtil.JAR_SEPARATOR}")
    }

    companion object {
        @JvmStatic
        @CoreEnvironmentDeprecation
        fun create(projectDisposable: Disposable, configuration: CompilerConfiguration): KotlinCoreEnvironment {
            setupIdeaStandaloneExecution()
            val appEnv = KotlinCoreApplicationEnvironment.getOrCreateApplicationEnvironment(projectDisposable)
            val projectEnv = KotlinCoreProjectEnvironment(projectDisposable, appEnv, configuration)
            val environment = KotlinCoreEnvironment(projectEnv, configuration)

            return environment
        }

        @JvmStatic
        @CoreEnvironmentDeprecation
        fun create(projectEnvironment: KotlinCoreProjectEnvironment, configuration: CompilerConfiguration): KotlinCoreEnvironment {
            return KotlinCoreEnvironment(projectEnvironment, configuration)
        }
    }
}
