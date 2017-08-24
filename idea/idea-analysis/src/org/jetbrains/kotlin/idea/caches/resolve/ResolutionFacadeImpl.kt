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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.container.tryGetService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.AbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

internal class ResolutionFacadeImpl(
        private val projectFacade: ProjectResolutionFacade,
        private val moduleInfo: IdeaModuleInfo
) : ResolutionFacade {
    override val project: Project
        get() = projectFacade.project

    //TODO: ideally we would like to store moduleDescriptor once and for all
    // but there are some usages that use resolutionFacade and mutate the psi leading to recomputation of underlying structures
    override val moduleDescriptor: ModuleDescriptor
        get() = findModuleDescriptor(moduleInfo)

    fun findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo) = projectFacade.findModuleDescriptor(ideaModuleInfo)

    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        return analyze(listOf(element), bodyResolveMode)
    }

    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        if (elements.isEmpty()) return BindingContext.EMPTY
        val resolveElementCache = getFrontendService(elements.first(), ResolveElementCache::class.java)
        return resolveElementCache.resolveToElements(elements, bodyResolveMode)
    }

    override fun analyzeFullyAndGetResult(elements: Collection<KtElement>): AnalysisResult
            = projectFacade.getAnalysisResultsForElements(elements)

    override fun resolveToDescriptor(declaration: KtDeclaration, bodyResolveMode: BodyResolveMode): DeclarationDescriptor {
        return if (KtPsiUtil.isLocal(declaration)) {
            val bindingContext = analyze(declaration, bodyResolveMode)
            bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
            ?: getFrontendService(moduleInfo, AbsentDescriptorHandler::class.java).diagnoseDescriptorNotFound(declaration)
        }
        else {
            val resolveSession = projectFacade.resolverForModuleInfo(declaration.getModuleInfo()).componentProvider.get<ResolveSession>()
            resolveSession.resolveToDescriptor(declaration)
        }
    }

    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T  = getFrontendService(moduleInfo, serviceClass)

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        return projectFacade.resolverForModuleInfo(moduleInfo).componentProvider.create(serviceClass)
    }

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        return getFrontendService(element.getModuleInfo(), serviceClass)
    }

    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
        return projectFacade.resolverForModuleInfo(element.getModuleInfo()).componentProvider.tryGetService(serviceClass)
    }

    fun <T : Any> getFrontendService(ideaModuleInfo: IdeaModuleInfo, serviceClass: Class<T>): T {
        return projectFacade.resolverForModuleInfo(ideaModuleInfo).componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return projectFacade.resolverForDescriptor(moduleDescriptor).componentProvider.getService(serviceClass)
    }
}

fun ResolutionFacade.findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo): ModuleDescriptor? {
    return (this as? ResolutionFacadeImpl)?.findModuleDescriptor(ideaModuleInfo)
}
