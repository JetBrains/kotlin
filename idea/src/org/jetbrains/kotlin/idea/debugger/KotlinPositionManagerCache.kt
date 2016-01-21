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
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JetTypeMapper
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

class KotlinPositionManagerCache(private val project: Project) {

    private val cachedClassNames = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<HashMap<PsiElement, List<String>>>(
                        hashMapOf(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    private val cachedTypeMappers = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<HashMap<PsiElement, JetTypeMapper>>(
                        hashMapOf(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    companion object {
        fun getOrComputeClassNames(psiElement: PsiElement, create: (PsiElement) -> List<String>): List<String> {
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

        fun getOrCreateTypeMapper(psiElement: PsiElement): JetTypeMapper {
            val cache = getInstance(psiElement.project)
            synchronized(cache.cachedTypeMappers) {
                val typeMappersCache = cache.cachedTypeMappers.value

                val file = psiElement.containingFile as KtFile
                val isInLibrary = LibraryUtil.findLibraryEntry(file.virtualFile, file.project) != null

                if (!isInLibrary) {
                    // Key = file
                    val cachedValue = typeMappersCache[file]
                    if (cachedValue != null) return cachedValue

                    val newValue = createTypeMapperForSourceFile(file)
                    typeMappersCache[file] = newValue
                    return newValue
                }
                else {
                    // key = KtElement
                    val element = getElementToCreateTypeMapperForLibraryFile(psiElement)
                    val cachedValue = typeMappersCache[psiElement]
                    if (cachedValue != null) return cachedValue

                    val newValue = createTypeMapperForLibraryFile(element, file)
                    typeMappersCache[psiElement] = newValue
                    return newValue
                }
            }
        }

        private fun getInstance(project: Project) = ServiceManager.getService(project, KotlinPositionManagerCache::class.java)!!

        private fun createTypeMapperForLibraryFile(element: KtElement, file: KtFile): JetTypeMapper {
            val analysisResult = element.analyzeAndGetResult()

            val state = GenerationState(
                    file.project,
                    ClassBuilderFactories.THROW_EXCEPTION,
                    analysisResult.moduleDescriptor,
                    analysisResult.bindingContext,
                    listOf(file))
            state.beforeCompile()
            return state.typeMapper
        }

        private fun getElementToCreateTypeMapperForLibraryFile(element: PsiElement?) =
                if (element is KtElement) element else PsiTreeUtil.getParentOfType(element, KtElement::class.java)!!

        private fun createTypeMapperForSourceFile(file: KtFile): JetTypeMapper {
            val analysisResult = file.analyzeFullyAndGetResult()
            analysisResult.throwIfError()

            val state = GenerationState(
                    file.project,
                    ClassBuilderFactories.THROW_EXCEPTION,
                    analysisResult.moduleDescriptor,
                    analysisResult.bindingContext,
                    listOf(file))
            state.beforeCompile()
            return state.typeMapper
        }

        @TestOnly fun addTypeMapper(file: KtFile, typeMapper: JetTypeMapper) {
            getInstance(file.project).cachedTypeMappers.value[file] = typeMapper
        }
    }
}
