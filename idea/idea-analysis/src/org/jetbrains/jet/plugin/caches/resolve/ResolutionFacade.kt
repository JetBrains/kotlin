/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve

import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.jet.analyzer.AnalysisResult
import org.jetbrains.jet.lang.resolve.lazy.BodyResolveMode

public trait ResolutionFacade {

    public fun analyze(element: JetElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext

    public fun analyzeFullyAndGetResult(elements: Collection<JetElement>): AnalysisResult

    public fun resolveToDescriptor(declaration: JetDeclaration): DeclarationDescriptor

    public fun findModuleDescriptor(element: JetElement): ModuleDescriptor

    public fun <T> get(extension: CacheExtension<T>): T
}
