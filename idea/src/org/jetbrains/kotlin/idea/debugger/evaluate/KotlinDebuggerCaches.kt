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
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.MultiMap
import org.apache.log4j.Logger
import org.jetbrains.annotations.TestOnly
import org.jetbrains.eval4j.Value
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class KotlinDebuggerCaches(private val project: Project) {

    private val cachedCompiledData = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<MultiMap<String, CompiledDataDescriptor>>(
                        MultiMap.create(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    private val cachedClassNames = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<HashMap<PsiElement, List<String>>>(
                        hashMapOf(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    private val cachedTypeMappers = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<HashMap<PsiElement, KotlinTypeMapper>>(
                        hashMapOf(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    companion object {
        private val LOG = Logger.getLogger(KotlinDebuggerCaches::class.java)!!

        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinDebuggerCaches::class.java)!!

        fun getOrCreateCompiledData(
                codeFragment: KtCodeFragment,
                sourcePosition: SourcePosition,
                evaluationContext: EvaluationContextImpl,
                create: (KtCodeFragment, SourcePosition) -> CompiledDataDescriptor
        ): CompiledDataDescriptor {
            val evaluateExpressionCache = getInstance(codeFragment.project)

            return synchronized<CompiledDataDescriptor>(evaluateExpressionCache.cachedCompiledData) {
                val cache = evaluateExpressionCache.cachedCompiledData.value!!
                val text = "${codeFragment.importsToString()}\n${codeFragment.text}"

                val answer = cache[text].firstOrNull {
                    it.sourcePosition == sourcePosition || evaluateExpressionCache.canBeEvaluatedInThisContext(it, evaluationContext)
                }
                if (answer != null) return@synchronized answer

                val newCompiledData = create(codeFragment, sourcePosition)
                LOG.debug("Compile bytecode for ${codeFragment.text}")

                cache.putValue(text, newCompiledData)
                return@synchronized newCompiledData
            }
        }

        fun <T: PsiElement> getOrComputeClassNames(psiElement: T, create: (T) -> ComputedClassNames): List<String> {
            val cache = getInstance(runReadAction { psiElement.project })
            synchronized(cache.cachedClassNames) {
                val classNamesCache = cache.cachedClassNames.value

                val cachedValue = classNamesCache[psiElement]
                if (cachedValue != null) return cachedValue

                val computedClassNames = create(psiElement)

                if (computedClassNames.shouldBeCached) {
                    classNamesCache[psiElement] = computedClassNames.classNames
                }
                return computedClassNames.classNames
            }
        }

        fun getOrCreateTypeMapper(psiElement: PsiElement): KotlinTypeMapper {
            val cache = getInstance(runReadAction { psiElement.project })
            synchronized(cache.cachedTypeMappers) {
                val typeMappersCache = cache.cachedTypeMappers.value

                val file = runReadAction { psiElement.containingFile as KtFile }
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

        private fun createTypeMapperForLibraryFile(element: KtElement, file: KtFile): KotlinTypeMapper {
            return runReadAction {
                val analysisResult = element.analyzeAndGetResult()

                val state = GenerationState(
                        file.project,
                        ClassBuilderFactories.THROW_EXCEPTION,
                        analysisResult.moduleDescriptor,
                        analysisResult.bindingContext,
                        listOf(file))
                state.beforeCompile()
                state.typeMapper
            }
        }

        private fun getElementToCreateTypeMapperForLibraryFile(element: PsiElement?) =
                runReadAction { if (element is KtElement) element else PsiTreeUtil.getParentOfType(element, KtElement::class.java)!! }

        private fun createTypeMapperForSourceFile(file: KtFile): KotlinTypeMapper {
            return runReadAction {
                val analysisResult = file.analyzeFullyAndGetResult()
                analysisResult.throwIfError()

                val state = GenerationState(
                        file.project,
                        ClassBuilderFactories.THROW_EXCEPTION,
                        analysisResult.moduleDescriptor,
                        analysisResult.bindingContext,
                        listOf(file))
                state.beforeCompile()
                state.typeMapper
            }
        }

        @TestOnly fun addTypeMapper(file: KtFile, typeMapper: KotlinTypeMapper) {
            getInstance(file.project).cachedTypeMappers.value[file] = typeMapper
        }
    }

    private fun canBeEvaluatedInThisContext(compiledData: CompiledDataDescriptor, context: EvaluationContextImpl): Boolean {
        val frameVisitor = FrameVisitor(context)
        return compiledData.parameters.all { p ->
            val (name, jetType) = p
            val value = frameVisitor.findValue(name, asmType = null, checkType = false, failIfNotFound = false)
            if (value == null) return@all false

            val thisDescriptor = value.asmType.getClassDescriptor(project)
            val superClassDescriptor = jetType.constructor.declarationDescriptor as? ClassDescriptor
            return@all thisDescriptor != null && superClassDescriptor != null && runReadAction { DescriptorUtils.isSubclass(thisDescriptor, superClassDescriptor) }
        }
    }

    data class CompiledDataDescriptor(
            val bytecodes: ByteArray,
            val additionalClasses: List<Pair<String, ByteArray>>,
            val sourcePosition: SourcePosition,
            val parameters: ParametersDescriptor
    )

    class ParametersDescriptor : Iterable<Parameter> {
        private val list = ArrayList<Parameter>()

        fun add(name: String, jetType: KotlinType, value: Value? = null) {
            list.add(Parameter(name, jetType, value))
        }

        override fun iterator() = list.iterator()
    }

    data class Parameter(val callText: String, val type: KotlinType, val value: Value? = null)

    sealed class ComputedClassNames(val classNames: List<String>, val shouldBeCached: Boolean) {
        class CachedClassNames(classNames: List<String>): ComputedClassNames(classNames, true) {
            constructor(className: String?): this(className.toList())
        }

        class NonCachedClassNames(classNames: List<String>): ComputedClassNames(classNames, false) {
            constructor(className: String?): this(className.toList())
        }
    }
}

private fun String?.toList() = if (this == null) emptyList() else listOf(this)
