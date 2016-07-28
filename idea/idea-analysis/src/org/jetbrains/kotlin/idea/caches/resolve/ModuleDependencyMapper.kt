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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.kotlin.analyzer.AnalyzerFacade
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters

fun createModuleResolverProvider(
        debugName: String,
        project: Project,
        globalContext: GlobalContextImpl,
        sdk: Sdk?,
        analyzerFacade: AnalyzerFacade<JvmPlatformParameters>,
        syntheticFiles: Collection<KtFile>,
        delegateResolver: ResolverForProject<IdeaModuleInfo>,
        moduleFilter: (IdeaModuleInfo) -> Boolean,
        allModules: Collection<IdeaModuleInfo>?,
        builtIns: KotlinBuiltIns,
        dependencies: Collection<Any>
): ModuleResolverProvider {

    val allModuleInfos = (allModules ?: collectAllModuleInfosFromIdeaModel(project)).toHashSet()

    val syntheticFilesByModule = syntheticFiles.groupBy { it.getModuleInfo() }
    val syntheticFilesModules = syntheticFilesByModule.keys
    allModuleInfos.addAll(syntheticFilesModules)

    val modulesToCreateResolversFor = allModuleInfos.filter(moduleFilter)

    fun createResolverForProject(): ResolverForProject<IdeaModuleInfo> {
        val modulesContent = { module: IdeaModuleInfo ->
            ModuleContent(syntheticFilesByModule[module] ?: listOf(), module.contentScope())
        }

        val jvmPlatformParameters = JvmPlatformParameters {
            javaClass: JavaClass ->
            val psiClass = (javaClass as JavaClassImpl).psi
            psiClass.getNullableModuleInfo()
        }

        val resolverForProject = analyzerFacade.setupResolverForProject(
                debugName, globalContext.withProject(project), modulesToCreateResolversFor, modulesContent,
                jvmPlatformParameters, IdeaEnvironment, builtIns,
                delegateResolver, { m, c -> IDEPackagePartProvider(c.moduleContentScope) },
                sdk?.let { SdkInfo(project, it) }
        )
        return resolverForProject
    }

    val resolverForProject = createResolverForProject()

    return ModuleResolverProviderImpl(
            resolverForProject,
            builtIns,
            dependencies + listOf(globalContext.exceptionTracker)
    )
}

private fun collectAllModuleInfosFromIdeaModel(project: Project): List<IdeaModuleInfo> {
    val ideaModules = ModuleManager.getInstance(project).modules.toList()
    val modulesSourcesInfos = ideaModules.flatMap { listOf(it.productionSourceInfo(), it.testSourceInfo()) }

    //TODO: (module refactoring) include libraries that are not among dependencies of any module
    val ideaLibraries = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<LibraryOrderEntry>().map {
            it.library
        }
    }.filterNotNull().toSet()

    val librariesInfos = ideaLibraries.map { LibraryInfo(project, it) }

    val sdksFromModulesDependencies = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).orderEntries.filterIsInstance<JdkOrderEntry>().map {
            it.jdk
        }
    }

    val sdksInfos = (sdksFromModulesDependencies + getAllProjectSdks()).filterNotNull().toSet().map { SdkInfo(project, it) }

    val collectAllModuleInfos = modulesSourcesInfos + librariesInfos + sdksInfos
    return collectAllModuleInfos
}

fun getAllProjectSdks(): Collection<Sdk> {
    return ProjectJdkTable.getInstance().allJdks.toList()
}


interface ModuleResolverProvider {
    val resolverForProject: ResolverForProject<IdeaModuleInfo>
    val builtIns: KotlinBuiltIns
    val cacheDependencies: Collection<Any>
}

class ModuleResolverProviderImpl(
        override val resolverForProject: ResolverForProject<IdeaModuleInfo>,
        override val builtIns: KotlinBuiltIns,
        override val cacheDependencies: Collection<Any>) : ModuleResolverProvider
