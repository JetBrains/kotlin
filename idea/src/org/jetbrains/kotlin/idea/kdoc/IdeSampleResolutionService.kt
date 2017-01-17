/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class IdeSampleResolutionService(val project: Project) : SampleResolutionService {

    override fun resolveSample(context: BindingContext, fromDescriptor: DeclarationDescriptor, resolutionFacade: ResolutionFacade, qualifiedName: List<String>): Collection<DeclarationDescriptor> {
        val allScope = GlobalSearchScope.projectScope(project)
        val shortName = qualifiedName.lastOrNull() ?: return emptyList()
        val functions = KotlinFunctionShortNameIndex.getInstance().get(shortName, project, allScope)
        val targetFqName = FqName.fromSegments(qualifiedName)


        val descriptors = functions.asSequence()
                .filter { it.fqName == targetFqName }
                .map { it.resolveToDescriptor(BodyResolveMode.PARTIAL) } // TODO Filter out not visible due dependencies config descriptors
                .toList()
        return descriptors
    }
}