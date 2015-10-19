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

package org.jetbrains.kotlin.idea.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public interface ResolutionFacade {
    public val project: Project

    public fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext

    public fun analyzeFullyAndGetResult(elements: Collection<KtElement>): AnalysisResult

    public fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor

    public val moduleDescriptor: ModuleDescriptor

    // get service for the module this resolution was created for
    public fun <T : Any> getFrontendService(serviceClass: Class<T>): T

    public fun <T : Any> getIdeService(serviceClass: Class<T>): T

    // get service for the module defined by PsiElement/ModuleDescriptor passed as parameter
    public fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T

    public fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T

}

public inline fun <reified T : Any> ResolutionFacade.frontendService(): T
        = this.getFrontendService(javaClass<T>())

public inline fun <reified T : Any> ResolutionFacade.ideService(): T
        = this.getIdeService(javaClass<T>())