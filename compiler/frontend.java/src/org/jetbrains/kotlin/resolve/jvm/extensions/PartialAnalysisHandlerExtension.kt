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

package org.jetbrains.kotlin.resolve.jvm.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.resolve.lazy.DeclarationScopeProvider
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope

open class PartialAnalysisHandlerExtension : AnalysisHandlerExtension {
    open val analyzePartially: Boolean
        get() = true

    open val analyzeDefaultParameterValues: Boolean
        get() = false

    override fun doAnalysis(
            project: Project,
            module: ModuleDescriptor,
            projectContext: ProjectContext,
            files: Collection<KtFile>,
            bindingTrace: BindingTrace,
            componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (!analyzePartially) {
            return null
        }

        val resolveSession = componentProvider.get<ResolveSession>()
        val bodyResolver = componentProvider.get<BodyResolver>()
        val declarationScopeProvider = componentProvider.get<DeclarationScopeProvider>()
        val topDownAnalyzer = componentProvider.get<LazyTopDownAnalyzer>()

        val topDownAnalysisContext = TopDownAnalysisContext(
                TopDownAnalysisMode.TopLevelDeclarations, DataFlowInfo.EMPTY, declarationScopeProvider)

        for (file in files) {
            ForceResolveUtil.forceResolveAllContents(resolveSession.getFileAnnotations(file))
            topDownAnalyzer.resolveImportsInFile(file)
        }

        doForEachDeclaration(files) { declaration ->
            val descriptor = resolveSession.resolveToDescriptor(declaration)

            when (descriptor) {
                is ClassDescriptor -> {
                    ForceResolveUtil.forceResolveAllContents(descriptor)
                    ForceResolveUtil.forceResolveAllContents(descriptor.typeConstructor.supertypes)

                    if (declaration is KtClassOrObject && descriptor is ClassDescriptorWithResolutionScopes) {
                        bodyResolver.resolveSuperTypeEntryList(DataFlowInfo.EMPTY,
                                                               declaration,
                                                               descriptor,
                                                               descriptor.unsubstitutedPrimaryConstructor,
                                                               descriptor.scopeForConstructorHeaderResolution,
                                                               descriptor.scopeForMemberDeclarationResolution)
                    }
                }
                is PropertyDescriptor -> {
                    if (declaration is KtProperty) {
                        /* TODO Now we analyse body with anonymous object initializers. Check if we can't avoid it
                         * val a: Runnable = object : Runnable { ... } */
                        bodyResolver.resolveProperty(topDownAnalysisContext, declaration, descriptor)
                    }
                }
                is FunctionDescriptor -> {
                    if (declaration is KtPrimaryConstructor && (analyzeDefaultParameterValues || descriptor.isAnnotationConstructor())) {
                        val containingScope = descriptor.containingScope
                        if (containingScope != null) {
                            bodyResolver.resolveConstructorParameterDefaultValues(
                                topDownAnalysisContext.outerDataFlowInfo, bindingTrace,
                                declaration, descriptor as ConstructorDescriptor, containingScope
                            )
                        }
                    } else if (declaration is KtFunction && !declaration.hasDeclaredReturnType() && !declaration.hasBlockBody()) {
                        ForceResolveUtil.forceResolveAllContents(descriptor)
                    }
                }
            }
        }

        return AnalysisResult.Companion.success(bindingTrace.bindingContext, module, true)
    }

    private val DeclarationDescriptor.containingScope: LexicalScope?
        get() {
            val containingDescriptor = containingDeclaration ?: return null
            return when (containingDescriptor) {
                is ClassDescriptorWithResolutionScopes -> containingDescriptor.scopeForInitializerResolution
                is PackageFragmentDescriptor -> LexicalScope.Base(containingDescriptor.getMemberScope().memberScopeAsImportingScope(), this)
                else -> null
            }
        }

    private fun doForEachDeclaration(declaration: KtDeclaration, f: (KtDeclaration) -> Unit) {
        if (declaration !is KtAnonymousInitializer) {
            f(declaration)
        }

        if (declaration is KtClassOrObject) {
            val primaryConstructor = declaration.primaryConstructor
            if (primaryConstructor != null) {
                f(primaryConstructor)
            }

            declaration.declarations.forEach { doForEachDeclaration(it, f) }
        }
    }

    private fun doForEachDeclaration(files: Collection<KtFile>, f: (KtDeclaration) -> Unit) {
        for (file in files) {
            file.declarations.forEach { doForEachDeclaration(it, f) }
        }
    }

}