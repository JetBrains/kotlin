/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.jet.utils.keysToMap
import com.intellij.openapi.roots.LibraryOrderEntry
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.jet.analyzer.AnalyzerFacade
import org.jetbrains.jet.analyzer.ResolverForModule
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.storage.ExceptionTracker
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.analyzer.ResolverForProject
import org.jetbrains.jet.analyzer.ModuleContent
import org.jetbrains.jet.analyzer.EmptyResolverForProject
import org.jetbrains.jet.context.GlobalContextImpl

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
        val modulesContent = {(module: IdeaModuleInfo) ->
            ModuleContent(syntheticFilesByModule[module] ?: listOf(), module.contentScope())
        }

        val jvmPlatformParameters = JvmPlatformParameters {
            (javaClass: JavaClass) ->
            val psiClass = (javaClass as JavaClassImpl).getPsi()
            psiClass.getModuleInfo()
        }

        val resolverForProject = analyzerFacade.setupResolverForProject(
                globalContext, project, modulesToCreateResolversFor, modulesContent, jvmPlatformParameters, delegateProvider.resolverForProject
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
        ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance(javaClass<LibraryOrderEntry>()).map {
            it.getLibrary()
        }
    }.filterNotNull().toSet()

    val librariesInfos = ideaLibraries.map { LibraryInfo(project, it) }

    val ideaSdks = ideaModules.flatMap {
        ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance(javaClass<JdkOrderEntry>()).map {
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