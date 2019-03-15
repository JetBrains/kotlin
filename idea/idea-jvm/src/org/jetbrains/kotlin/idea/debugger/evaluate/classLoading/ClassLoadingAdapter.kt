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

package org.jetbrains.kotlin.idea.debugger.evaluate.classLoading

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.sun.jdi.ArrayReference
import com.sun.jdi.ArrayType
import com.sun.jdi.ClassLoaderReference
import com.sun.jdi.Value
import org.jetbrains.kotlin.idea.debugger.evaluate.GENERATED_FUNCTION_NAME
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.tree.*
import kotlin.math.min

interface ClassLoadingAdapter {
    companion object {
        private const val CHUNK_SIZE = 4096

        private val ADAPTERS = listOf(
            AndroidOClassLoadingAdapter(),
            OrdinaryClassLoadingAdapter()
        )

        fun loadClasses(context: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderReference? {
            val mainClass = classes.firstOrNull { it.isMainClass } ?: return null

            var info = ClassInfoForEvaluator(containsAdditionalClasses = classes.size > 1)
            if (!info.containsAdditionalClasses) {
                info = analyzeClass(mainClass, info)
            }

            for (adapter in ADAPTERS) {
                if (adapter.isApplicable(context, info)) {
                    return adapter.loadClasses(context, classes)
                }
            }

            return null
        }

        data class ClassInfoForEvaluator(
            val containsLoops: Boolean = false,
            val containsCodeUnsupportedInEval4J: Boolean = false,
            val containsAdditionalClasses: Boolean = false
        ) {
            val isCompilingEvaluatorPreferred: Boolean
                get() = containsLoops || containsCodeUnsupportedInEval4J || containsAdditionalClasses
        }

        private fun analyzeClass(classToLoad: ClassToLoad, info: ClassInfoForEvaluator): ClassInfoForEvaluator {
            val classNode = ClassNode().apply { ClassReader(classToLoad.bytes).accept(this, 0) }
            val methodToRun = classNode.methods.single { it.name == GENERATED_FUNCTION_NAME }

            val visitedLabels = hashSetOf<Label>()

            tailrec fun analyzeInsn(insn: AbstractInsnNode, info: ClassInfoForEvaluator): ClassInfoForEvaluator {
                when (insn) {
                    is LabelNode -> visitedLabels += insn.label
                    is JumpInsnNode -> {
                        if (insn.label.label in visitedLabels) {
                            return info.copy(containsLoops = true)
                        }
                    }
                    is TableSwitchInsnNode, is LookupSwitchInsnNode -> {
                        return info.copy(containsCodeUnsupportedInEval4J = true)
                    }
                }

                val nextInsn = insn.next ?: return info
                return analyzeInsn(nextInsn, info)
            }

            val firstInsn = methodToRun.instructions?.first ?: return info
            return analyzeInsn(firstInsn, info)
        }
    }

    fun isApplicable(context: EvaluationContextImpl, info: ClassInfoForEvaluator): Boolean

    fun loadClasses(context: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderReference

    fun mirrorOfByteArray(bytes: ByteArray, context: EvaluationContextImpl, process: DebugProcessImpl): ArrayReference {
        val arrayClass = process.findClass(context, "byte[]", context.classLoader) as ArrayType
        val reference = process.newInstance(arrayClass, bytes.size)
        DebuggerUtilsEx.keep(reference, context)

        val mirrors = ArrayList<Value>(bytes.size)
        for (byte in bytes) {
            mirrors += process.virtualMachineProxy.mirrorOf(byte)
        }

        var loaded = 0
        while (loaded < mirrors.size) {
            val chunkSize = min(CHUNK_SIZE, mirrors.size - loaded)
            reference.setValues(loaded, mirrors, loaded, chunkSize)
            loaded += chunkSize
        }

        return reference
    }
}