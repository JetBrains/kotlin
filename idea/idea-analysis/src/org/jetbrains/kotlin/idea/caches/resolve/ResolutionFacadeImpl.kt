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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompositeBindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

private class ResolutionFacadeImpl(
        private val project: Project,
        computeModuleResolverProvider: () -> CachedValueProvider.Result<ModuleResolverProvider>
) : ResolutionFacade {
    private val resolverCache = SynchronizedCachedValue(project, computeModuleResolverProvider, trackValue = false)

    val moduleResolverProvider: ModuleResolverProvider
        get() = resolverCache.getValue()

    public fun getLazyResolveSession(element: JetElement): ResolveSession {
        return resolverForModuleInfo(element.getModuleInfo()).componentProvider.get<ResolveSession>()
    }

    private fun resolverForModuleInfo(moduleInfo: IdeaModuleInfo) = moduleResolverProvider.resolverForProject.resolverForModule(moduleInfo)
    private fun resolverForDescriptor(moduleDescriptor: ModuleDescriptor) = moduleResolverProvider.resolverForProject.resolverForModuleDescriptor(moduleDescriptor)

    override fun <T> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        return resolverForModuleInfo(element.getModuleInfo()).componentProvider.getService(serviceClass)
    }

    override fun <T> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return resolverForDescriptor(moduleDescriptor).componentProvider.getService(serviceClass)
    }

    override fun <T> getIdeService(element: PsiElement, serviceClass: Class<T>): T {
        return resolverForModuleInfo(element.getModuleInfo()).componentProvider.create(serviceClass)
    }

    override fun <T> getIdeService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return resolverForDescriptor(moduleDescriptor).componentProvider.create(serviceClass)
    }

    override fun analyze(element: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        val resolveElementCache = getFrontendService(element, javaClass<ResolveElementCache>())
        return resolveElementCache.resolveToElement(element, bodyResolveMode)
    }

    override fun findModuleDescriptor(element: JetElement): ModuleDescriptor {
        return moduleResolverProvider.resolverForProject.descriptorForModule(element.getModuleInfo())
    }

    override fun resolveToDescriptor(declaration: JetDeclaration): DeclarationDescriptor {
        return getLazyResolveSession(declaration).resolveToDescriptor(declaration)
    }

    override fun analyzeFullyAndGetResult(elements: Collection<JetElement>): AnalysisResult {
        return getAnalysisResultsForElements(elements)
    }

    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue(
            {
                val resolverProvider = moduleResolverProvider
                val results = object : SLRUCache<JetFile, PerFileAnalysisCache>(2, 3) {
                    override fun createValue(file: JetFile?): PerFileAnalysisCache {
                        return PerFileAnalysisCache(file!!, resolverProvider.resolverForProject.resolverForModule(file.getModuleInfo()).componentProvider)
                    }
                }
                CachedValueProvider.Result(results, PsiModificationTracker.MODIFICATION_COUNT, resolverProvider.exceptionTracker)
            }, false)

    fun getAnalysisResultsForElements(elements: Collection<JetElement>): AnalysisResult {
        assert(elements.isNotEmpty(), "elements collection should not be empty")
        val slruCache = synchronized(analysisResults) {
            analysisResults.getValue()!!
        }
        val results = elements.map {
            val perFileCache = synchronized(slruCache) {
                slruCache[it.getContainingJetFile()]
            }
            perFileCache.getAnalysisResults(it)
        }
        val withError = results.firstOrNull { it.isError() }
        val bindingContext = CompositeBindingContext.create(results.map { it.bindingContext })
        return if (withError != null)
            AnalysisResult.error(bindingContext, withError.error)
        else
        //TODO: (module refactoring) several elements are passed here in debugger
            AnalysisResult.success(bindingContext, getLazyResolveSession(elements.first()).getModuleDescriptor())
    }
}