/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.extensions.AreaInstance
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
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

class CliFe10AnalysisFacade : Fe10AnalysisFacade {
    override fun getResolveSession(element: KtElement): ResolveSession {
        return getHandler(element).resolveSession ?: error("Resolution is not performed")
    }

    override fun getDeprecationResolver(element: KtElement): DeprecationResolver {
        return getHandler(element).deprecationResolver ?: error("Resolution is not performed")
    }

    override fun getCallResolver(element: KtElement): CallResolver {
        return getHandler(element).callResolver ?: error("Resolution is not performed")
    }

    override fun getKotlinToResolvedCallTransformer(element: KtElement): KotlinToResolvedCallTransformer {
        return getHandler(element).kotlinToResolvedCallTransformer ?: error("Resolution is not performed")
    }

    override fun getOverloadingConflictResolver(element: KtElement): OverloadingConflictResolver<ResolvedCall<*>> {
        return getHandler(element).overloadingConflictResolver ?: error("Resolution is not performed")
    }

    override fun getKotlinTypeRefiner(element: KtElement): KotlinTypeRefiner {
        return getHandler(element).kotlinTypeRefiner ?: error("Resolution is not performed")
    }

    override fun analyze(element: KtElement, mode: Fe10AnalysisFacade.AnalysisMode): BindingContext {
        return getResolveSession(element).bindingContext
    }

    override fun getOrigin(file: VirtualFile): KtSymbolOrigin {
        return KtSymbolOrigin.SOURCE
    }

    private fun getHandler(useSiteElement: KtElement): KtFe10AnalysisHandlerExtension {
        val project = useSiteElement.project
        val ktModule = ProjectStructureProvider.getModule(project, useSiteElement, contextualModule = null)
        return KtFe10AnalysisHandlerExtension.getInstance(project, ktModule)
    }
}

class KtFe10AnalysisHandlerExtension(private val useSiteModule: KtSourceModule) : AnalysisHandlerExtension {
    internal companion object {
        fun getInstance(area: AreaInstance, module: KtModule): KtFe10AnalysisHandlerExtension {
            return AnalysisHandlerExtension.extensionPointName.getExtensions(area)
                .filterIsInstance<KtFe10AnalysisHandlerExtension>()
                .firstOrNull { it.useSiteModule == module }
                ?: error(KtFe10AnalysisHandlerExtension::class.java.name + " should be registered")
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
        if (module.name.asString().removeSurrounding("<", ">") != useSiteModule.moduleName) {
            // there is no way to properly map KtModule to ModuleDescriptor,
            // KtFe10AnalysisHandlerExtension is used only for tests, so just by name comparasion should work as all module names are different
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