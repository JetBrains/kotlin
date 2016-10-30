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

@file:JvmName("ResolutionUtils")

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun KtElement.getResolutionFacade(): ResolutionFacade {
    return KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this))
}

fun KtDeclaration.resolveToDescriptor(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor {
    return getResolutionFacade().resolveToDescriptor(this, bodyResolveMode)
}

//TODO: BodyResolveMode.PARTIAL is not quite safe!
fun KtDeclaration.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): DeclarationDescriptor? {
    return analyze(bodyResolveMode).get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
}

fun KtFile.resolveImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
    val facade = getResolutionFacade()
    return facade.resolveImportReference(facade.moduleDescriptor, fqName)
}

//NOTE: the difference between analyze and analyzeFully is 'intentionally' unclear
// in theory they do the same thing via different code
// analyze - see ResolveSessionForBodies, ResolveElementCache
// analyzeFully - see KotlinResolveCache, KotlinResolveDataProvider
// In the future these two approaches should be unified
@JvmOverloads fun KtElement.analyze(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext {
    return getResolutionFacade().analyze(this, bodyResolveMode)
}

fun KtElement.analyzeAndGetResult(): AnalysisResult {
    val resolutionFacade = getResolutionFacade()
    return AnalysisResult.success(resolutionFacade.analyze(this), resolutionFacade.moduleDescriptor)
}

fun KtElement.findModuleDescriptor(): ModuleDescriptor {
    return getResolutionFacade().moduleDescriptor
}

fun KtElement.analyzeFully(): BindingContext {
    return analyzeFullyAndGetResult().bindingContext
}

fun KtElement.analyzeFullyAndGetResult(vararg extraFiles: KtFile): AnalysisResult {
    return KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this) + extraFiles.toList()).analyzeFullyAndGetResult(listOf(this))
}

// this method don't check visibility and collect all descriptors with given fqName
fun ResolutionFacade.resolveImportReference(
        moduleDescriptor: ModuleDescriptor,
        fqName: FqName
): Collection<DeclarationDescriptor> {
    val importDirective = KtPsiFactory(project).createImportDirective(ImportPath(fqName, false))
    val qualifiedExpressionResolver = this.getFrontendService(moduleDescriptor, QualifiedExpressionResolver::class.java)
    return qualifiedExpressionResolver.processImportReference(
            importDirective, moduleDescriptor, BindingTraceContext(), excludedImportNames = emptyList(), packageFragmentForVisibilityCheck = null)?.getContributedDescriptors() ?: emptyList()
}