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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.analyzer.ResolverForProjectImpl
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformSupport
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
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
    allModules: Collection<IdeaModuleInfo>,
    providedBuiltIns: KotlinBuiltIns?, // null means create new builtins based on SDK
    dependencies: Collection<Any>,
    invalidateOnOOCB: Boolean = true
): ModuleResolverProvider {
    val builtIns = providedBuiltIns ?: createBuiltIns(analysisSettings, globalContext)

    val allModuleInfos = allModules.toMutableSet()

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
        packageOracleFactory = ServiceManager.getService(project, IdePackageOracleFactory::class.java),
        languageSettingsProvider = IDELanguageSettingsProvider,
        invalidateOnOOCB = invalidateOnOOCB
    )

    if (providedBuiltIns == null && builtIns is JvmBuiltIns) {
        val sdkModuleDescriptor = analysisSettings.sdk!!.let { resolverForProject.descriptorForModule(
            SdkInfo(
                project,
                it
            )
        ) }
        builtIns.initialize(sdkModuleDescriptor, analysisSettings.isAdditionalBuiltInFeaturesSupported)
    }

    return ModuleResolverProvider(
        resolverForProject,
        builtIns,
        dependencies + listOf(globalContext.exceptionTracker)
    )
}

private fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
    val supportInstance = IdePlatformSupport.platformSupport[settings.platform] ?: return DefaultBuiltIns.Instance
    return supportInstance.createBuiltIns(settings, sdkContext)
}

class ModuleResolverProvider(
    val resolverForProject: ResolverForProject<IdeaModuleInfo>,
    val builtIns: KotlinBuiltIns,
    val cacheDependencies: Collection<Any>
)
