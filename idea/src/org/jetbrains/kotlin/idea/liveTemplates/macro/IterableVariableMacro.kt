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

package org.jetbrains.kotlin.idea.liveTemplates.macro

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.IterableTypesDetection
import org.jetbrains.kotlin.idea.core.IterableTypesDetector
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext

class IterableVariableMacro : BaseKotlinVariableMacro() {
    private companion object {
        val ITERABLE_TYPES_DETECTOR_KEY = Key<IterableTypesDetector>("ITERABLE_TYPES_DETECTOR_KEY")
    }

    override fun getName() = "kotlinIterableVariable"
    override fun getPresentableName() = "kotlinIterableVariable()"

    override fun initUserData(userData: UserDataHolder, contextElement: KtElement, bindingContext: BindingContext) {
        val resolutionFacade = contextElement.getResolutionFacade()
        val scope = contextElement.getResolutionScope(bindingContext, resolutionFacade)
        val detector = resolutionFacade.getIdeService(IterableTypesDetection::class.java).createDetector(scope)
        userData.putUserData(ITERABLE_TYPES_DETECTOR_KEY, detector)
    }

    override fun isSuitable(variableDescriptor: VariableDescriptor, project: Project, userData: UserDataHolder): Boolean {
        val detector = userData.getUserData(ITERABLE_TYPES_DETECTOR_KEY)!!
        //TODO: smart-casts
        return detector.isIterable(variableDescriptor.type, null)
    }
}
