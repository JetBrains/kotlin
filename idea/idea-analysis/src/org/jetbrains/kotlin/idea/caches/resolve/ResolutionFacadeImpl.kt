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
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.JetScope

private class ResolutionFacadeImpl(private val project: Project, private val cache: KotlinResolveCache) : ResolutionFacade {
    override fun <T> getIdeService(element: JetElement, serviceClass: Class<T>): T {
        return cache.getIdeService(element, serviceClass)
    }

    override fun <T> getIdeService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return cache.getIdeService(moduleDescriptor, serviceClass)
    }

    override fun analyze(element: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        val resolveElementCache = cache.getService(element, javaClass<ResolveElementCache>())
        return resolveElementCache.resolveToElement(element, bodyResolveMode)
    }

    override fun findModuleDescriptor(element: JetElement): ModuleDescriptor {
        return cache.getLazyResolveSession(element).getModuleDescriptor()
    }

    override fun resolveToDescriptor(declaration: JetDeclaration): DeclarationDescriptor {
        return cache.getLazyResolveSession(declaration).resolveToDescriptor(declaration)
    }

    override fun analyzeFullyAndGetResult(elements: Collection<JetElement>): AnalysisResult {
        return cache.getAnalysisResultsForElements(elements)
    }

    override fun getFileTopLevelScope(file: JetFile): JetScope {
        return cache.getLazyResolveSession(file).getFileScopeProvider().getFileScope(file)
    }

    override fun <T> getFrontendService(element: JetElement, serviceClass: Class<T>): T {
        return cache.getService(element, serviceClass)
    }

    override fun <T> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return cache.getService(moduleDescriptor, serviceClass)
    }

    override fun resolveImportReference(moduleDescriptor: ModuleDescriptor, fqName: FqName): Collection<DeclarationDescriptor> {
        val importDirective = JetPsiFactory(project).createImportDirective(ImportPath(fqName, false))
        val qualifiedExpressionResolver = this.getService<QualifiedExpressionResolver>(moduleDescriptor)
        return qualifiedExpressionResolver.processImportReference(
                importDirective, moduleDescriptor, BindingTraceContext(), QualifiedExpressionResolver.LookupMode.EVERYTHING).getAllDescriptors()
    }

    override fun <T> get(extension: CacheExtension<T>): T {
        return cache[extension]
    }
}