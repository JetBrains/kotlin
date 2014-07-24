/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger.evaluate

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.jet.lang.psi.JetCodeFragment
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import java.util.ArrayList
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.org.objectweb.asm.Type
import com.intellij.util.containers.MultiMap
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.plugin.caches.resolve.JavaResolveExtension
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import org.jetbrains.jet.codegen.AsmUtil
import org.apache.log4j.Logger

class KotlinEvaluateExpressionCache(val project: Project) {

    private val cachedCompiledData = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<MultiMap<String, CompiledDataDescriptor>>(
                        MultiMap.create(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    class object {
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

            val thisDescriptor = value.asmType.getClassDescriptor()
            val superClassDescriptor = jetType.getConstructor().getDeclarationDescriptor() as? ClassDescriptor
            return@all thisDescriptor != null && superClassDescriptor != null && DescriptorUtils.isSubclass(thisDescriptor, superClassDescriptor)
        }
    }

    private fun Type.getClassDescriptor(): ClassDescriptor? {
        if (AsmUtil.isPrimitive(this)) return null

        val jvmName = JvmClassName.byInternalName(getInternalName()).getFqNameForClassNameWithoutDollars()

        val platformClasses = JavaToKotlinClassMap.getInstance().mapPlatformClass(jvmName)
        if (platformClasses.notEmpty) return platformClasses.first()

        return ApplicationManager.getApplication()?.runReadAction<ClassDescriptor> {
            val classes = JavaPsiFacade.getInstance(project).findClasses(jvmName.asString(), GlobalSearchScope.allScope(project))
            if (classes.isEmpty()) null else JavaResolveExtension[project].resolveClass(JavaClassImpl(classes.first()))
        }
    }

    data class CompiledDataDescriptor(val bytecodes: ByteArray, val sourcePosition: SourcePosition, val funName: String, val parameters: ParametersDescriptor)

    class ParametersDescriptor : Iterable<Pair<String, JetType>> {
        private val list = ArrayList<Pair<String, JetType>>()

        fun add(name: String, jetType: JetType) {
            list.add(name to jetType)
        }

        fun getParameterNames() = list.map { it.first }

        override fun iterator() = list.iterator()
    }
}