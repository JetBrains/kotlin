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

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public fun JetElement.getResolutionFacade(): ResolutionFacade {
    return KotlinCacheService.getInstance(getProject()).getResolutionFacade(listOf(this))
}

public fun JetDeclaration.resolveToDescriptor(): DeclarationDescriptor {
    return getResolutionFacade().resolveToDescriptor(this)
}

public fun JetFile.resolveImportReference(fqName: FqName, isDefaultImport: Boolean = false): Collection<DeclarationDescriptor> {
    val facade = getResolutionFacade()
    return facade.resolveImportReference(facade.findModuleDescriptor(this), fqName, isDefaultImport)
}

//NOTE: the difference between analyze and analyzeFully is 'intentionally' unclear
// in theory they do the same thing via different code
// analyze - see ResolveSessionForBodies, ResolveElementCache
// analyzeFully - see KotlinResolveCache, KotlinResolveDataProvider
// In the future these two approaches should be unified
@jvmOverloads
public fun JetElement.analyze(bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext {
    return getResolutionFacade().analyze(this, bodyResolveMode)
}

public fun JetElement.analyzeAndGetResult(): AnalysisResult {
    val resolutionFacade = getResolutionFacade()
    return AnalysisResult.success(resolutionFacade.analyze(this), resolutionFacade.findModuleDescriptor(this))
}

public fun JetElement.findModuleDescriptor(): ModuleDescriptor {
    return getResolutionFacade().findModuleDescriptor(this)
}

public fun JetElement.analyzeFully(): BindingContext {
    return analyzeFullyAndGetResult().bindingContext
}

public fun JetElement.analyzeFullyAndGetResult(vararg extraFiles: JetFile): AnalysisResult {
    return KotlinCacheService.getInstance(getProject()).getResolutionFacade(listOf(this) + extraFiles.toList()).analyzeFullyAndGetResult(listOf(this))
}
