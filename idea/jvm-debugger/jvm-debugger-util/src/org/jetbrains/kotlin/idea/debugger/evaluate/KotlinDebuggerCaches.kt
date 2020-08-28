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

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.MultiMap
import org.apache.log4j.Logger
import org.jetbrains.annotations.TestOnly
import org.jetbrains.eval4j.Value
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContentAndGetResult
import org.jetbrains.kotlin.idea.core.util.runInReadActionWithWriteActionPriorityWithPCE
import org.jetbrains.kotlin.idea.debugger.BinaryCacheKey
import org.jetbrains.kotlin.idea.debugger.BytecodeDebugInfo
import org.jetbrains.kotlin.idea.debugger.createWeakBytecodeDebugInfoStorage
import org.jetbrains.kotlin.idea.debugger.evaluate.compilation.CompiledDataDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.runReadActionInSmartMode
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.KotlinType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class KotlinDebuggerCaches(project: Project) {
    private val cachedCompiledData = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result<MultiMap<String, CompiledDataDescriptor>>(
                MultiMap.create(), PsiModificationTracker.MODIFICATION_COUNT
            )
        }, false
    )

    private val cachedClassNames = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result<MutableMap<PsiElement, List<String>>>(
                ConcurrentHashMap(),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }, false
    )

    private val cachedTypeMappers = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result<MutableMap<PsiElement, KotlinTypeMapper>>(
                ConcurrentHashMap(),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }, false
    )

    private val debugInfoCache = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result(
                createWeakBytecodeDebugInfoStorage(),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }, false
    )

    companion object {
        private val LOG = Logger.getLogger(KotlinDebuggerCaches::class.java)!!

        @get:TestOnly
        var LOG_COMPILATIONS: Boolean = false

        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinDebuggerCaches::class.java)!!

        fun compileCodeFragmentCacheAware(
            codeFragment: KtCodeFragment,
            sourcePosition: SourcePosition?,
            compileCode: () -> CompiledDataDescriptor,
            force: Boolean = false
        ): Pair<CompiledDataDescriptor, Boolean> {
            if (sourcePosition == null) {
                return Pair(compileCode(), false)
            }

            val evaluateExpressionCache = getInstance(codeFragment.project)

            val text = "${codeFragment.importsToString()}\n${codeFragment.text}"

            val cachedResults = synchronized<Collection<CompiledDataDescriptor>>(evaluateExpressionCache.cachedCompiledData) {
                evaluateExpressionCache.cachedCompiledData.value[text]
            }

            val existingResult = cachedResults.firstOrNull { it.sourcePosition == sourcePosition }
            if (existingResult != null) {
                if (force) {
                    synchronized(evaluateExpressionCache.cachedCompiledData) {
                        evaluateExpressionCache.cachedCompiledData.value.remove(text, existingResult)
                    }
                } else {
                    return Pair(existingResult, true)
                }
            }

            val newCompiledData = compileCode()

            if (LOG_COMPILATIONS) {
                LOG.debug("Compile bytecode for ${codeFragment.text}")
            }

            synchronized(evaluateExpressionCache.cachedCompiledData) {
                evaluateExpressionCache.cachedCompiledData.value.putValue(text, newCompiledData)
            }

            return Pair(newCompiledData, false)
        }

        fun <T : PsiElement> getOrComputeClassNames(psiElement: T?, create: (T) -> ComputedClassNames): List<String> {
            if (psiElement == null) return Collections.emptyList()

            val cache = getInstance(runReadAction { psiElement.project })

            val classNamesCache = cache.cachedClassNames.value

            val cachedValue = classNamesCache[psiElement]
            if (cachedValue != null) return cachedValue

            val computedClassNames = create(psiElement)

            if (computedClassNames.shouldBeCached) {
                classNamesCache[psiElement] = computedClassNames.classNames
            }

            return computedClassNames.classNames
        }

        fun getOrCreateTypeMapper(psiElement: PsiElement): KotlinTypeMapper {
            val project = runReadAction { psiElement.project }
            val cache = getInstance(project)

            val file = runReadAction { psiElement.containingFile as KtFile }
            val isInLibrary = runReadAction { LibraryUtil.findLibraryEntry(file.virtualFile, file.project) } != null

            val key = if (!isInLibrary) file else psiElement

            val typeMappersCache = cache.cachedTypeMappers.value

            val cachedValue = typeMappersCache[key]
            if (cachedValue != null) return cachedValue

            val newValue = project.runReadActionInSmartMode {
                if (!isInLibrary) {
                    createTypeMapperForSourceFile(file)
                } else {
                    val element = getElementToCreateTypeMapperForLibraryFile(psiElement)
                    createTypeMapperForLibraryFile(element, file)
                }
            }

            typeMappersCache[key] = newValue
            return newValue
        }

        fun getOrReadDebugInfoFromBytecode(
            project: Project,
            jvmName: JvmClassName,
            file: VirtualFile
        ): BytecodeDebugInfo? {
            val cache = getInstance(project)
            return cache.debugInfoCache.value[BinaryCacheKey(project, jvmName, file)]
        }

        private fun getElementToCreateTypeMapperForLibraryFile(element: PsiElement?) =
            runReadAction { element as? KtElement ?: PsiTreeUtil.getParentOfType(element, KtElement::class.java)!! }

        private fun createTypeMapperForLibraryFile(element: KtElement, file: KtFile): KotlinTypeMapper =
            runInReadActionWithWriteActionPriorityWithPCE {
                createTypeMapper(file, element.analyzeWithContentAndGetResult())
            }

        private fun createTypeMapperForSourceFile(file: KtFile): KotlinTypeMapper =
            runInReadActionWithWriteActionPriorityWithPCE {
                createTypeMapper(file, file.analyzeWithAllCompilerChecks().apply(AnalysisResult::throwIfError))
            }

        private fun createTypeMapper(file: KtFile, analysisResult: AnalysisResult): KotlinTypeMapper {
            val state = GenerationState.Builder(
                file.project,
                ClassBuilderFactories.THROW_EXCEPTION,
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                listOf(file),
                CompilerConfiguration.EMPTY
            ).build()
            state.beforeCompile()
            return state.typeMapper
        }

        @TestOnly
        fun addTypeMapper(file: KtFile, typeMapper: KotlinTypeMapper) {
            getInstance(file.project).cachedTypeMappers.value[file] = typeMapper
        }
    }

    data class Parameter(val callText: String, val type: KotlinType, val value: Value? = null, val error: EvaluateException? = null)

    class ComputedClassNames(val classNames: List<String>, val shouldBeCached: Boolean) {
        @Suppress("FunctionName")
        companion object {
            val EMPTY = Cached(emptyList())

            fun Cached(classNames: List<String>) = ComputedClassNames(classNames, true)
            fun Cached(className: String) = ComputedClassNames(Collections.singletonList(className), true)

            fun NonCached(classNames: List<String>) = ComputedClassNames(classNames, false)
        }

        fun isEmpty() = classNames.isEmpty()

        fun distinct() = ComputedClassNames(classNames.distinct(), shouldBeCached)

        operator fun plus(other: ComputedClassNames) = ComputedClassNames(
            classNames + other.classNames, shouldBeCached && other.shouldBeCached
        )
    }
}