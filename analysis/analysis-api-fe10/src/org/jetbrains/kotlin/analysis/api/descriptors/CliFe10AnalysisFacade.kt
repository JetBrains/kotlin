/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.extensions.AreaInstance
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadingConflictResolver
import org.jetbrains.kotlin.resolve.calls.results.PlatformOverloadsSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.results.createOverloadingConflictResolver
import org.jetbrains.kotlin.resolve.calls.tower.KotlinToResolvedCallTransformer
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.util.CancellationChecker

internal class CliFe10AnalysisFacade : Fe10AnalysisFacade {
    override fun getAnalysisContext(element: KtElement, token: KaLifetimeToken): Fe10AnalysisContext {
        val handler = getHandler(element)
        return getAnalysisContext(handler, token)
    }

    override fun getAnalysisContext(ktModule: KaModule, token: KaLifetimeToken): Fe10AnalysisContext {
        val handler = KaFe10AnalysisHandlerExtension.getInstance(ktModule.project, ktModule)
        return getAnalysisContext(handler, token)
    }

    private fun getAnalysisContext(handler: KaFe10AnalysisHandlerExtension, token: KaLifetimeToken): Fe10AnalysisContext {
        return Fe10AnalysisContext(
            facade = this,
            handler.resolveSession.orThrowResolutionNotPerformedError(),
            handler.deprecationResolver.orThrowResolutionNotPerformedError(),
            handler.callResolver.orThrowResolutionNotPerformedError(),
            handler.kotlinToResolvedCallTransformer.orThrowResolutionNotPerformedError(),
            handler.overloadingConflictResolver.orThrowResolutionNotPerformedError(),
            handler.kotlinTypeRefiner.orThrowResolutionNotPerformedError(),
            token,
        )
    }

    override fun analyze(elements: List<KtElement>, mode: Fe10AnalysisFacade.AnalysisMode): BindingContext {
        val element = elements.firstOrNull() ?: return BindingContext.EMPTY
        return getHandler(element).resolveSession.orThrowResolutionNotPerformedError().bindingContext
    }

    override fun getOrigin(file: VirtualFile): KaSymbolOrigin {
        return KaSymbolOrigin.SOURCE
    }

    private fun getHandler(useSiteElement: KtElement): KaFe10AnalysisHandlerExtension {
        val project = useSiteElement.project
        val ktModule = KotlinProjectStructureProvider.getModule(project, useSiteElement, useSiteModule = null)
        return KaFe10AnalysisHandlerExtension.getInstance(project, ktModule)
    }

    private fun <T : Any> T?.orThrowResolutionNotPerformedError(): T =
        this ?: error("Resolution is not performed")
}

internal class KaFe10AnalysisHandlerExtension(
    private val useSiteModule: KaSourceModule? = null
) : AnalysisHandlerExtension {
    internal companion object {
        fun getInstance(area: AreaInstance, module: KaModule): KaFe10AnalysisHandlerExtension {
            val extensions = AnalysisHandlerExtension.extensionPointName.getExtensions(area)
                .filterIsInstance<KaFe10AnalysisHandlerExtension>()
            return extensions.firstOrNull { it.useSiteModule == module }
                ?: extensions.singleOrNull { it.useSiteModule == null }
                ?: error(KaFe10AnalysisHandlerExtension::class.java.name + " should be registered")
        }
    }

    var resolveSession: ResolveSession? = null
        private set

    var deprecationResolver: DeprecationResolver? = null
        private set

    var callResolver: CallResolver? = null
        private set

    var kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer? = null
        private set

    var overloadingConflictResolver: OverloadingConflictResolver<ResolvedCall<*>>? = null
        private set

    var kotlinTypeRefiner: KotlinTypeRefiner? = null
        private set

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        // Single-module [KtFe10AnalysisHandlerExtension] can be registered without specific use-site module.
        // Simple null-check below will skip the bail-out.
        if (useSiteModule != null &&
            module.name.asString().removeSurrounding("<", ">") != useSiteModule.name
        ) {
            // there is no way to properly map KaModule to ModuleDescriptor,
            // Multi-module [KtFe10AnalysisHandlerExtension]s are used only for tests,
            // so just by name comparison should work as all module names are different
            return null
        }

        resolveSession = componentProvider.get()
        deprecationResolver = componentProvider.get()
        callResolver = componentProvider.get()
        kotlinToResolvedCallTransformer = componentProvider.get()
        kotlinTypeRefiner = componentProvider.get()

        val builtIns = resolveSession!!.moduleDescriptor.builtIns
        val typeSpecificityComparator = componentProvider.get<TypeSpecificityComparator>()
        val platformOverloadsSpecificityComparator = componentProvider.get<PlatformOverloadsSpecificityComparator>()
        val cancellationChecker = componentProvider.get<CancellationChecker>()
        overloadingConflictResolver = createOverloadingConflictResolver(
            builtIns,
            module,
            typeSpecificityComparator,
            platformOverloadsSpecificityComparator,
            cancellationChecker,
            kotlinTypeRefiner!!
        )
        return super.doAnalysis(project, module, projectContext, files, bindingTrace, componentProvider)
    }
}