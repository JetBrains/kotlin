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

import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.analyzer.ResolverForProjectImpl
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformSupport
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters

fun createModuleResolverProvider(
        debugName: String,
        project: Project,
        globalContext: GlobalContextImpl,
        analysisSettings: PlatformAnalysisSettings,
        syntheticFiles: Collection<KtFile>,
        delegateResolver: ResolverForProject<IdeaModuleInfo>,
        moduleFilter: (IdeaModuleInfo) -> Boolean,
        allModules: Collection<IdeaModuleInfo>?,
        providedBuiltIns: KotlinBuiltIns?, // null means create new builtins based on SDK
        dependencies: Collection<Any>,
        invalidateOnOOCB: Boolean = true
): ModuleResolverProvider {
    val builtIns = providedBuiltIns ?: createBuiltIns(analysisSettings, globalContext)

    val allModuleInfos = (allModules ?: collectAllModuleInfosFromIdeaModel(project)).toHashSet()

    val syntheticFilesByModule = syntheticFiles.groupBy(KtFile::getModuleInfo)
    val syntheticFilesModules = syntheticFilesByModule.keys
    allModuleInfos.addAll(syntheticFilesModules)

    val modulesToCreateResolversFor = allModuleInfos.filter(moduleFilter)

    val modulesContent = { module: IdeaModuleInfo ->
        ModuleContent(syntheticFilesByModule[module] ?: listOf(), module.contentScope())
    }

    val jvmPlatformParameters = JvmPlatformParameters { javaClass: JavaClass ->
        val psiClass = (javaClass as JavaClassImpl).psi
        psiClass.getNullableModuleInfo()
    }

    val resolverForProject = ResolverForProjectImpl(
            debugName, globalContext.withProject(project), modulesToCreateResolversFor,
            { module ->
                val platform = module.platform ?: analysisSettings.platform
                IdePlatformSupport.facades[platform] ?: throw UnsupportedOperationException("Unsupported platform $platform")
            },
            modulesContent, jvmPlatformParameters,
            IdeaEnvironment, builtIns,
            delegateResolver, { _, c -> IDEPackagePartProvider(c.moduleContentScope) },
            analysisSettings.sdk?.let { SdkInfo(project, it) },
            modulePlatforms = { module -> module.platform?.multiTargetPlatform },
            packageOracleFactory = project.service<IdePackageOracleFactory>(),
            languageSettingsProvider =  IDELanguageSettingsProvider,
            invalidateOnOOCB = invalidateOnOOCB
    )

    if (providedBuiltIns == null && builtIns is JvmBuiltIns) {
        val sdkModuleDescriptor = analysisSettings.sdk!!.let { resolverForProject.descriptorForModule(SdkInfo(project, it)) }
        builtIns.initialize(sdkModuleDescriptor, analysisSettings.isAdditionalBuiltInFeaturesSupported)
    }

    return ModuleResolverProvider(
            resolverForProject,
            builtIns,
            dependencies + listOf(globalContext.exceptionTracker)
    )
}

fun collectAllModuleInfosFromIdeaModel(project: Project): List<IdeaModuleInfo> {
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

    return modulesSourcesInfos + librariesInfos + sdksInfos
}

private fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
    val supportInstance = IdePlatformSupport.platformSupport[settings.platform] ?: return DefaultBuiltIns.Instance
    return supportInstance.createBuiltIns(settings, sdkContext)
}

fun getAllProjectSdks(): Collection<Sdk> {
    return ProjectJdkTable.getInstance().allJdks.toList()
}


class ModuleResolverProvider(
        val resolverForProject: ResolverForProject<IdeaModuleInfo>,
        val builtIns: KotlinBuiltIns,
        val cacheDependencies: Collection<Any>
)
