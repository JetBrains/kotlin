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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

interface UsageReplacementStrategy {
    fun createReplacer(usage: JetSimpleNameExpression): (() -> JetElement)?

    companion object {
        fun build(element: JetSimpleNameExpression, replaceWith: ReplaceWith, recheckAnnotation: Boolean): UsageReplacementStrategy? {
            val resolutionFacade = element.getResolutionFacade()
            val bindingContext = resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)
            var target = element.mainReference.resolveToDescriptors(bindingContext).singleOrNull() ?: return null

            var replacePatternFromSymbol = DeprecatedSymbolUsageFixBase.fetchReplaceWithPattern(target, resolutionFacade.project)
            if (replacePatternFromSymbol == null && target is ConstructorDescriptor) {
                target = target.containingDeclaration
                replacePatternFromSymbol = DeprecatedSymbolUsageFixBase.fetchReplaceWithPattern(target, resolutionFacade.project)
            }

            // check that ReplaceWith hasn't changed
            if (recheckAnnotation && replacePatternFromSymbol != replaceWith) return null

            when (target) {
                is CallableDescriptor -> {
                    val resolvedCall = element.getResolvedCall(bindingContext) ?: return null
                    if (!resolvedCall.isReallySuccess()) return null
                    val replacement = ReplaceWithAnnotationAnalyzer.analyzeCallableReplacement(replaceWith, target, resolutionFacade) ?: return null
                    return CallableUsageReplacementStrategy(replacement)
                }

                is ClassDescriptor -> {
                    val replacementType = ReplaceWithAnnotationAnalyzer.analyzeClassReplacement(replaceWith, target, resolutionFacade)
                    if (replacementType != null) { //TODO: check that it's really resolved and is not an object otherwise it can be expression as well
                        return ClassUsageReplacementStrategy(replacementType, null, element.project)
                    }
                    else {
                        val constructor = target.unsubstitutedPrimaryConstructor ?: return null
                        val replacementExpression = ReplaceWithAnnotationAnalyzer.analyzeCallableReplacement(replaceWith, constructor, resolutionFacade) ?: return null
                        return ClassUsageReplacementStrategy(null, replacementExpression, element.project)
                    }
                }

                else -> return null
            }
        }
    }
}