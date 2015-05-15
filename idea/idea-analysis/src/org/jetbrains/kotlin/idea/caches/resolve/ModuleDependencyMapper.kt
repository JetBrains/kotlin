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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.idea.project.ResolveSessionForBodies
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.utils.keysToMap

fun createModuleResolverProvider(
        project: Project,
        globalContext: GlobalContextImpl,
        analyzerFacade: AnalyzerFacade<out ResolverForModule, JvmPlatformParameters>,
        syntheticFiles: Collection<JetFile>,
        delegateProvider: ModuleResolverProvider,
        moduleFilter: (IdeaModuleInfo) -> Boolean
): ModuleResolverProvider {

    val allModuleInfos = collectAllModuleInfosFromIdeaModel(project).toHashSet()

    val syntheticFilesByModule = syntheticFiles.groupBy { it.getModuleInfo() }
    val syntheticFilesModules = syntheticFilesByModule.keySet()
    allModuleInfos.addAll(syntheticFilesModules)

    val modulesToCreateResolversFor = allModuleInfos.filter(moduleFilter)

    fun createResolverForProject(): ResolverForProject<IdeaModuleInfo, ResolverForModule> {
        val modulesContent = { module: IdeaModuleInfo ->
            ModuleContent(syntheticFilesByModule[module] ?: listOf(), module.contentScope())
        }

        val jvmPlatformParameters = JvmPlatformParameters {
            javaClass: JavaClass ->
            val psiClass = (javaClass as JavaClassImpl).getPsi()
            psiClass.getModuleInfo()
        }

        val resolverForProject = analyzerFacade.setupResolverForProject(
                globalContext.withProject(project), modulesToCreateResolversFor, modulesContent,
                jvmPlatformParameters, delegateProvider.resolverForProject
        )
        return resolverForProject
    }

    val resolverForProject = createResolverForProject()

    val moduleToBodiesResolveSession = modulesToCreateResolversFor.keysToMap {
        module ->
        val analyzer = resolverForProject.resolverForModule(module)
        ResolveSessionForBodies(project, analyzer.lazyResolveSession)
    }
    return ModuleResolverProviderImpl(
            resolverForProject,
            moduleToBodiesResolveSession,
            globalContext,
            delegateProvider
    )
}

private fun collectAllModuleInfosFromIdeaModel(project: Project): List<IdeaModuleInfo> {
    val ideaModules = ModuleManager.getInstance(project).getModules().toList()
    val modulesSourcesInfos = ideaModules.flatMap { listOf(it.productionSourceInfo(), it.testSourceInfo()) }

    //TODO: (module refactoring) include libraries that are not among dependencies of any module
    val ideaLibraries = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance<LibraryOrderEntry>().map {
            it.getLibrary()
        }
    }.filterNotNull().toSet()

    val librariesInfos = ideaLibraries.map { LibraryInfo(project, it) }

    val ideaSdks = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance<JdkOrderEntry>().map {
            it.getJdk()
        }
    }.filterNotNull().toSet()

    val sdksInfos = ideaSdks.map { SdkInfo(project, it) }

    val collectAllModuleInfos = modulesSourcesInfos + librariesInfos + sdksInfos
    return collectAllModuleInfos
}

trait ModuleResolverProvider {
    val exceptionTracker: ExceptionTracker
    fun resolverByModule(module: IdeaModuleInfo): ResolverForModule = resolverForProject.resolverForModule(module)
    fun resolveSessionForBodiesByModule(module: IdeaModuleInfo): ResolveSessionForBodies
    val resolverForProject: ResolverForProject<IdeaModuleInfo, ResolverForModule>
}

object EmptyModuleResolverProvider: ModuleResolverProvider {
    override val exceptionTracker: ExceptionTracker
        get() = throw IllegalStateException("Should not be called")

    override fun resolveSessionForBodiesByModule(module: IdeaModuleInfo): ResolveSessionForBodies
            = throw IllegalStateException("Trying to obtain resolve session for unknown $module")

    override val resolverForProject: ResolverForProject<IdeaModuleInfo, ResolverForModule> = EmptyResolverForProject()

}

class ModuleResolverProviderImpl(
        override val resolverForProject: ResolverForProject<IdeaModuleInfo, ResolverForModule>,
        private val bodiesResolveByModule: Map<IdeaModuleInfo, ResolveSessionForBodies>,
        val globalContext: GlobalContextImpl,
        val delegateProvider: ModuleResolverProvider = EmptyModuleResolverProvider
): ModuleResolverProvider {
    override val exceptionTracker: ExceptionTracker = globalContext.exceptionTracker

    override fun resolveSessionForBodiesByModule(module: IdeaModuleInfo): ResolveSessionForBodies =
            bodiesResolveByModule[module] ?:
            delegateProvider.resolveSessionForBodiesByModule(module)
}
