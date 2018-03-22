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
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.IdePlatformSupport
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompositeBindingContext
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal class ProjectResolutionFacade(
    private val debugString: String,
    private val resolverDebugName: String,
    val project: Project,
    val globalContext: GlobalContextImpl,
    val settings: PlatformAnalysisSettings,
    val reuseDataFrom: ProjectResolutionFacade?,
    val moduleFilter: (IdeaModuleInfo) -> Boolean,
    dependencies: List<Any>,
    private val invalidateOnOOCB: Boolean,
    val syntheticFiles: Collection<KtFile> = listOf(),
    val allModules: Collection<IdeaModuleInfo>? = null // null means create resolvers for modules from idea model
) {
    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
        {
            val resolverProvider = computeModuleResolverProvider()
            CachedValueProvider.Result.create(resolverProvider, resolverForProjectDependencies)
        },
        /* trackValue = */ false
    )

    private val cachedResolverForProject: ResolverForProject<IdeaModuleInfo>
        get() = globalContext.storageManager.compute { cachedValue.value }

    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue(
        {
            val resolverForProject = cachedResolverForProject
            val results = object : SLRUCache<KtFile, PerFileAnalysisCache>(2, 3) {
                override fun createValue(file: KtFile): PerFileAnalysisCache {
                    return PerFileAnalysisCache(
                        file,
                        resolverForProject.resolverForModule(file.getModuleInfo()).componentProvider
                    )
                }
            }

            val allDependencies = resolverForProjectDependencies + listOf(PsiModificationTracker.MODIFICATION_COUNT)
            CachedValueProvider.Result.create(results, allDependencies)
        }, false
    )

    private val resolverForProjectDependencies = dependencies + listOf(globalContext.exceptionTracker)

    private fun computeModuleResolverProvider(): ResolverForProject<IdeaModuleInfo> {
        val delegateResolverForProject: ResolverForProject<IdeaModuleInfo>
        val delegateBuiltIns: KotlinBuiltIns?

        if (reuseDataFrom != null) {
            delegateResolverForProject = reuseDataFrom.cachedResolverForProject
            delegateBuiltIns = delegateResolverForProject.builtIns
        } else {
            delegateResolverForProject = EmptyResolverForProject()
            delegateBuiltIns = null
        }

        val builtIns = delegateBuiltIns ?: createBuiltIns(
            settings,
            globalContext
        )

        val allModuleInfos = (allModules ?: collectAllModuleInfosFromIdeaModel(project)).toMutableSet()

        val syntheticFilesByModule = syntheticFiles.groupBy(KtFile::getModuleInfo)
        val syntheticFilesModules = syntheticFilesByModule.keys
        allModuleInfos.addAll(syntheticFilesModules)

        val modulesToCreateResolversFor = allModuleInfos.filter(moduleFilter)

        val modulesContentFactory = { module: IdeaModuleInfo ->
            ModuleContent(syntheticFilesByModule[module] ?: listOf(), module.contentScope())
        }

        val jvmPlatformParameters = JvmPlatformParameters { javaClass: JavaClass ->
            val psiClass = (javaClass as JavaClassImpl).psi
            psiClass.getNullableModuleInfo()
        }

        val resolverForProject = ResolverForProjectImpl(
            resolverDebugName,
            globalContext.withProject(project),
            modulesToCreateResolversFor,
            { module ->
                val platform = module.platform ?: settings.platform
                IdePlatformSupport.facades[platform] ?: throw UnsupportedOperationException("Unsupported platform $platform")
            },
            modulesContentFactory,
            jvmPlatformParameters,
            IdeaEnvironment,
            builtIns,
            delegateResolverForProject,
            packagePartProviderFactory = { _, c -> IDEPackagePartProvider(c.moduleContentScope) },
            firstDependency = settings.sdk?.let { SdkInfo(project, it) },
            modulePlatforms = { module -> module.platform?.multiTargetPlatform },
            packageOracleFactory = ServiceManager.getService(project, IdePackageOracleFactory::class.java),
            languageSettingsProvider = IDELanguageSettingsProvider,
            invalidateOnOOCB = invalidateOnOOCB
        )

        if (delegateBuiltIns == null && builtIns is JvmBuiltIns) {
            val sdkModuleDescriptor = settings.sdk!!.let {
                resolverForProject.descriptorForModule(
                    SdkInfo(project, it)
                )
            }
            builtIns.initialize(sdkModuleDescriptor, settings.isAdditionalBuiltInFeaturesSupported)
        }

        return resolverForProject
    }

    fun resolverForModuleInfo(moduleInfo: IdeaModuleInfo) = cachedResolverForProject.resolverForModule(moduleInfo)

    fun resolverForElement(element: PsiElement): ResolverForModule {
        val infos = element.getModuleInfos()
        return infos.asIterable().firstNotNullResult { cachedResolverForProject.tryGetResolverForModule(it) }
                ?: cachedResolverForProject.tryGetResolverForModule(NotUnderContentRootModuleInfo)
                ?: cachedResolverForProject.diagnoseUnknownModuleInfo(infos.toList())
    }

    fun resolverForDescriptor(moduleDescriptor: ModuleDescriptor) = cachedResolverForProject.resolverForModuleDescriptor(moduleDescriptor)

    fun findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo): ModuleDescriptor {
        return cachedResolverForProject.descriptorForModule(ideaModuleInfo)
    }

    fun getAnalysisResultsForElements(elements: Collection<KtElement>): AnalysisResult {
        assert(elements.isNotEmpty()) { "elements collection should not be empty" }
        val slruCache = synchronized(analysisResults) {
            analysisResults.value!!
        }
        val results = elements.map {
            val perFileCache = synchronized(slruCache) {
                slruCache[it.containingKtFile]
            }
            perFileCache.getAnalysisResults(it)
        }
        val withError = results.firstOrNull { it.isError() }
        val bindingContext = CompositeBindingContext.create(results.map { it.bindingContext })
        if (withError != null) {
            return AnalysisResult.internalError(bindingContext, withError.error)
        }

        //TODO: (module refactoring) several elements are passed here in debugger
        return AnalysisResult.success(bindingContext, findModuleDescriptor(elements.first().getModuleInfo()))
    }

    override fun toString(): String {
        return "$debugString@${Integer.toHexString(hashCode())}"
    }

    companion object {
        private fun createBuiltIns(settings: PlatformAnalysisSettings, sdkContext: GlobalContextImpl): KotlinBuiltIns {
            val supportInstance = IdePlatformSupport.platformSupport[settings.platform] ?: return DefaultBuiltIns.Instance
            return supportInstance.createBuiltIns(settings, sdkContext)
        }
    }
}
