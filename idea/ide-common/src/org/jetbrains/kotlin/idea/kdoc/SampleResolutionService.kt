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

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.resolve.BindingContext

interface SampleResolutionService {
    fun resolveSample(context: BindingContext, fromDescriptor: DeclarationDescriptor, resolutionFacade: ResolutionFacade, qualifiedName: List<String>): Collection<DeclarationDescriptor>

    companion object {

        /**
         * It's internal implementation, please use [resolveKDocSampleLink], or [resolveKDocLink]
         */
        internal fun resolveSample(context: BindingContext, fromDescriptor: DeclarationDescriptor, resolutionFacade: ResolutionFacade, qualifiedName: List<String>): Collection<DeclarationDescriptor> {
            val instance = ServiceManager.getService(resolutionFacade.project, SampleResolutionService::class.java)
            return instance?.resolveSample(context, fromDescriptor, resolutionFacade, qualifiedName) ?: emptyList()
        }
    }
}
