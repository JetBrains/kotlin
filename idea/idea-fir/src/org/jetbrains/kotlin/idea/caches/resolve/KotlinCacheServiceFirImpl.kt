/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


class ResolutionFacadeFirImpl(
    override val project: Project,
    val moduleInfo: IdeaModuleInfo,
    val platform: TargetPlatform
) : ResolutionFacade {
    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext = FirBasedBindingContext()

    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext = FirBasedBindingContext()

    override fun analyzeWithAllCompilerChecks(elements: Collection<KtElement>): AnalysisResult {
        TODO("Not yet implemented")
    }

    override fun resolveToDescriptor(declaration: KtDeclaration, bodyResolveMode: BodyResolveMode): DeclarationDescriptor {
        TODO("Not yet implemented")
    }

    override val moduleDescriptor: ModuleDescriptor
        get() = TODO("Not yet implemented")

    @FrontendInternals
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        TODO("Not yet implemented")
    }

    @FrontendInternals
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        TODO("Not yet implemented")
    }

    @FrontendInternals
    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        TODO("Not yet implemented")
    }

    @FrontendInternals
    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
        TODO("Not yet implemented")
    }

    override fun getResolverForProject(): ResolverForProject<out ModuleInfo> {
        TODO("Not yet implemented")
    }

}
class KotlinCacheServiceFirImpl(val project: Project) : KotlinCacheService {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        val firstFile = elements.first().containingKtFile
        val platform = TargetPlatformDetector.getPlatform(firstFile)
        return getResolutionFacadeByModuleInfo(firstFile.getModuleInfo(), platform)
    }

    override fun getResolutionFacade(elements: List<KtElement>, platform: TargetPlatform): ResolutionFacade {
        val firstFile = elements.first().containingKtFile
        return getResolutionFacadeByModuleInfo(firstFile.getModuleInfo(), platform)
    }

    override fun getResolutionFacadeByFile(file: PsiFile, platform: TargetPlatform): ResolutionFacade? =
        getResolutionFacadeByModuleInfo(file.getModuleInfo(), platform)

    override fun getSuppressionCache(): KotlinSuppressCache {
        TODO("Not yet implemented")
    }

    override fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo, platform: TargetPlatform): ResolutionFacade? =
        (moduleInfo as? IdeaModuleInfo)?.let { getResolutionFacadeByModuleInfo(it, platform) }

    private fun getResolutionFacadeByModuleInfo(moduleInfo: IdeaModuleInfo, platform: TargetPlatform): ResolutionFacade {
        return ResolutionFacadeFirImpl(project, moduleInfo, platform)
    }
}