/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.fe10

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.fir.fe10.binding.KtSymbolBasedBindingContext
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.BindingContextSuppressCache
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class KtSymbolBasedResolutionFacade(
    override val project: Project,
    val context: FE10BindingContext
) : ResolutionFacade {
    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext = KtSymbolBasedBindingContext(context)

    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext =
        KtSymbolBasedBindingContext(context)

    override fun analyzeWithAllCompilerChecks(elements: Collection<KtElement>, callback: DiagnosticSink.DiagnosticsCallback?): AnalysisResult {
        return AnalysisResult.success(KtSymbolBasedBindingContext(context), context.moduleDescriptor)
    }

    override fun resolveToDescriptor(declaration: KtDeclaration, bodyResolveMode: BodyResolveMode): DeclarationDescriptor {
        val ktSymbol = context.withAnalysisSession {
            declaration.getSymbol()
        }
        return ktSymbol.toDeclarationDescriptor(context)
    }

    override val moduleDescriptor: ModuleDescriptor
        get() = context.moduleDescriptor

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

class KtSymbolBasedKotlinCacheServiceImpl(val project: Project) : KotlinCacheService {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade =
        KtSymbolBasedResolutionFacade(project, FE10BindingContextImpl(project, elements.first()))

    // todo: platform are ignored
    override fun getResolutionFacade(elements: List<KtElement>, platform: TargetPlatform): ResolutionFacade =
        KtSymbolBasedResolutionFacade(project, FE10BindingContextImpl(project, elements.first()))

    override fun getResolutionFacadeByFile(file: PsiFile, platform: TargetPlatform): ResolutionFacade? =
        file.safeAs<KtFile>()?.let { KtSymbolBasedResolutionFacade(project, FE10BindingContextImpl(project, it)) }

    override fun getSuppressionCache(): KotlinSuppressCache {
        return BindingContextSuppressCache(BindingContext.EMPTY)
    }

    override fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo, platform: TargetPlatform): ResolutionFacade? =
        TODO("Not yet implemented")

    override fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo, settings: PlatformAnalysisSettings): ResolutionFacade? {
        TODO("Not yet implemented")
    }
}