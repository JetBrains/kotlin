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

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.psi.JetCodeFragment
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import java.util.ArrayList
import com.intellij.openapi.components.ServiceManager
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.apache.log4j.Logger
import org.jetbrains.kotlin.idea.util.application.runReadAction

class KotlinEvaluateExpressionCache(val project: Project) {

    private val cachedCompiledData = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<MultiMap<String, CompiledDataDescriptor>>(
                        MultiMap.create(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    default object {
        private val LOG = Logger.getLogger(javaClass<KotlinEvaluateExpressionCache>())!!

        fun getInstance(project: Project) = ServiceManager.getService(project, javaClass<KotlinEvaluateExpressionCache>())!!

        fun getOrCreateCompiledData(
                codeFragment: JetCodeFragment,
                sourcePosition: SourcePosition,
                evaluationContext: EvaluationContextImpl,
                create: (JetCodeFragment, SourcePosition) -> CompiledDataDescriptor
        ): CompiledDataDescriptor {
            val evaluateExpressionCache = getInstance(codeFragment.getProject())

            return synchronized(evaluateExpressionCache.cachedCompiledData) {
                (): CompiledDataDescriptor ->
                val cache = evaluateExpressionCache.cachedCompiledData.getValue()!!
                val text = "${codeFragment.importsToString()}\n${codeFragment.getText()}"

                val answer = cache[text].firstOrNull {
                    it.sourcePosition == sourcePosition || evaluateExpressionCache.canBeEvaluatedInThisContext(it, evaluationContext)
                }
                if (answer != null) return@synchronized answer

                val newCompiledData = create(codeFragment, sourcePosition)
                LOG.debug("Compile bytecode for ${codeFragment.getText()}")

                cache.putValue(text, newCompiledData)
                return@synchronized newCompiledData
            }
        }
    }

    private fun canBeEvaluatedInThisContext(compiledData: CompiledDataDescriptor, context: EvaluationContextImpl): Boolean {
        return compiledData.parameters.all { (p): Boolean ->
            val (name, jetType) = p
            val value = context.findLocalVariable(name, asmType = null, checkType = false, failIfNotFound = false)
            if (value == null) return@all false

            val thisDescriptor = value.asmType.getClassDescriptor(project)
            val superClassDescriptor = jetType.getConstructor().getDeclarationDescriptor() as? ClassDescriptor
            return@all thisDescriptor != null && superClassDescriptor != null && runReadAction { DescriptorUtils.isSubclass(thisDescriptor, superClassDescriptor) }!!
        }
    }

    data class CompiledDataDescriptor(
            val bytecodes: ByteArray,
            val additionalClasses: List<Pair<String, ByteArray>>,
            val sourcePosition: SourcePosition,
            val funName: String,
            val parameters: ParametersDescriptor
    )

    class ParametersDescriptor : Iterable<Pair<String, JetType>> {
        private val list = ArrayList<Pair<String, JetType>>()

        fun add(name: String, jetType: JetType) {
            list.add(name to jetType)
        }

        fun getParameterNames() = list.map { it.first }

        override fun iterator() = list.iterator()
    }
}
