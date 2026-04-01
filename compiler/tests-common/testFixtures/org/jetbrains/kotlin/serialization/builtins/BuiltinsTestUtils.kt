/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForSingleModuleProject
import org.jetbrains.kotlin.analyzer.TrackableModuleInfo
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.jvm.JvmResolverForModuleFactory
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.regex.Pattern

object BuiltinsTestUtils {
    fun compileBuiltinsModule(environment: KotlinCoreEnvironment): ModuleDescriptor {
        val files = KotlinTestUtils.loadToKtFiles(
            environment, ContainerUtil.concat<File>(
                allFilesUnder("libraries/stdlib/jvm/"),
                allFilesUnder("libraries/stdlib/src/")
            )
        ).filter {
            it.annotationEntries.any { annotation ->
                annotation.shortName == StandardClassIds.Annotations.JvmBuiltin.shortClassName
            }
        }
        return createResolveSessionForFiles(environment.project, files, false).moduleDescriptor
    }

    @JvmField
    val BUILTIN_PACKAGE_NAMES = listOf(
        StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
        StandardNames.COLLECTIONS_PACKAGE_FQ_NAME,
        StandardNames.RANGES_PACKAGE_FQ_NAME
    )

    private fun allFilesUnder(directory: String): List<File?> {
        return FileUtil.findFilesByMask(Pattern.compile(".*\\.kt"), File(directory))
    }

    private fun createResolveSessionForFiles(
        project: Project,
        syntheticFiles: Collection<KtFile>,
        addBuiltIns: Boolean
    ): ResolveSession {
        val projectContext = ProjectContext(project, "lazy resolve test utils")
        val testModule =
            TestModule(project, addBuiltIns)
        val platformParameters = JvmPlatformParameters(
            packagePartProviderFactory = { PackagePartProvider.Empty },
            moduleByJavaClass = { testModule },
            useBuiltinsProviderForModule = { false }
        )

        val resolverForProject = ResolverForSingleModuleProject(
            "test",
            projectContext,
            testModule,
            JvmResolverForModuleFactory(platformParameters, CompilerEnvironment, JvmPlatforms.defaultJvmPlatform),
            GlobalSearchScope.allScope(project),
            syntheticFiles = syntheticFiles
        )

        return resolverForProject.resolverForModule(testModule).componentProvider.get<ResolveSession>()
    }

    private class TestModule(val project: Project, val dependsOnBuiltIns: Boolean) : TrackableModuleInfo {
        override val name: Name = Name.special("<Test module for lazy resolve>")

        override fun dependencies() = listOf(this)
        override fun dependencyOnBuiltIns() =
            if (dependsOnBuiltIns)
                ModuleInfo.DependencyOnBuiltIns.LAST
            else
                ModuleInfo.DependencyOnBuiltIns.NONE

        override val platform: TargetPlatform
            get() = JvmPlatforms.unspecifiedJvmPlatform

        override fun createModificationTracker(): ModificationTracker {
            return ModificationTracker.NEVER_CHANGED
        }

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = JvmPlatformAnalyzerServices
    }
}
