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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import java.util.*

class KotlinPositionManagerCache(private val project: Project) {

    private val cachedClassNames = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<HashMap<PsiElement, Collection<String>>>(
                        hashMapOf(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    companion object {
        fun getOrComputeClassNames(psiElement: PsiElement, create: (PsiElement) -> Collection<String>): Collection<String> {
            val cache = getInstance(psiElement.project)
            synchronized(cache.cachedClassNames) {
                val classNamesCache = cache.cachedClassNames.value

                val cachedValue = classNamesCache[psiElement]
                if (cachedValue != null) return cachedValue

                val newValue = create(psiElement)

                classNamesCache[psiElement] = newValue
                return newValue
            }
        }

        private fun getInstance(project: Project) = ServiceManager.getService(project, KotlinPositionManagerCache::class.java)!!
    }
}
