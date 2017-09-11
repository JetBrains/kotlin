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

fun KtElement.getResolutionFacade(): ResolutionFacade =
        KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this))

fun KtDeclaration.resolveToDescriptor(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor =
        getResolutionFacade().resolveToDescriptor(this, bodyResolveMode)

//TODO: BodyResolveMode.PARTIAL is not quite safe!
fun KtDeclaration.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL): DeclarationDescriptor? =
        analyze(bodyResolveMode).get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)

fun KtFile.resolveImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
    val facade = getResolutionFacade()
    return facade.resolveImportReference(facade.moduleDescriptor, fqName)
}


// This and next function are used for 'normal' element analysis
// Their exact semantics is a bit unclear and depends on 'bodyResolveMode'
// They are expected to provide correct descriptors for the element
// but not diagnostics, trace slices are provided only partially
// Element body analysis, if any, is not guaranteed
// For compiler-compatible analysis, analyzeFully is recommended
// See ResolveSessionForBodies, ResolveElementCache
@JvmOverloads fun KtElement.analyze(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext =
        getResolutionFacade().analyze(this, bodyResolveMode)

fun KtElement.analyzeAndGetResult(): AnalysisResult {
    val resolutionFacade = getResolutionFacade()
    return AnalysisResult.success(resolutionFacade.analyze(this), resolutionFacade.moduleDescriptor)
}

fun KtElement.findModuleDescriptor(): ModuleDescriptor = getResolutionFacade().moduleDescriptor

// This and next function are expected to produce the same result as compiler
// for the given element and its children (including diagnostics, trace slices, descriptors, etc.)
// Not recommended to call both of them without real need
// See also KotlinResolveCache, KotlinResolveDataProvider
// In the future should be unified with 'analyze`
fun KtElement.analyzeFully(): BindingContext = analyzeFullyAndGetResult().bindingContext

fun KtElement.analyzeFullyAndGetResult(vararg extraFiles: KtFile): AnalysisResult =
        KotlinCacheService.getInstance(project).getResolutionFacade(listOf(this) + extraFiles.toList()).analyzeFullyAndGetResult(listOf(this))

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