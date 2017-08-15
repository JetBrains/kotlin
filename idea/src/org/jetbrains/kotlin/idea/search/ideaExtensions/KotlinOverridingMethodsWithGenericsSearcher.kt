/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.getDirectlyOverriddenDeclarations
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class KotlinOverridingMethodsWithGenericsSearcher : QueryExecutor<PsiMethod, OverridingMethodsSearch.SearchParameters> {
    override fun execute(p: OverridingMethodsSearch.SearchParameters, consumer: Processor<PsiMethod>): Boolean {
        val method = p.method
        if (method !is KtLightMethod) return true

        val declaration = method.kotlinOrigin as? KtCallableDeclaration ?: return true

        val callDescriptor = runReadAction { declaration.resolveToDescriptor() }
        if (callDescriptor !is CallableDescriptor) return true

        // Java overriding method search can't find overloads with primitives types, so
        // we do additional search for such methods.
        if (!callDescriptor.valueParameters.any { it.type.constructor.declarationDescriptor is TypeParameterDescriptor }) return true

        val parentClass = runReadAction { method.containingClass }

        return ClassInheritorsSearch.search(parentClass, p.scope, true).forEach(Processor { inheritor: PsiClass ->
            val found = runReadAction {
                findOverridingMethod(inheritor, declaration)
            }

            found == null || (consumer.process(found) && p.isCheckDeep)
        })
    }

    private fun findOverridingMethod(inheritor: PsiClass, callableDeclaration: KtCallableDeclaration): PsiMethod? {
        // Leave Java classes search to JavaOverridingMethodsSearcher
        if (inheritor !is KtLightClass) return null

        val name = callableDeclaration.name
        val methodsByName = inheritor.findMethodsByName(name, false)

        for (lightMethodCandidate in methodsByName) {
            val candidateDescriptor = (lightMethodCandidate as? KtLightMethod)?.kotlinOrigin?.resolveToDescriptor() ?: continue
            if (candidateDescriptor !is CallableMemberDescriptor) continue

            val overriddenDescriptors = candidateDescriptor.getDirectlyOverriddenDeclarations()
            for (candidateSuper in overriddenDescriptors) {
                val candidateDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(candidateSuper)
                if (candidateDeclaration == callableDeclaration) {
                    return lightMethodCandidate
                }
            }
        }

        return null
    }
}
