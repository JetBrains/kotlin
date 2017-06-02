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
import com.sun.jdi.Value

interface ClassLoadingAdapter {
    companion object {
        private val ADAPTERS = listOf(
                AndroidOClassLoadingAdapter(),
                OrdinaryClassLoadingAdapter())

        fun loadClasses(context: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderHandler {
            for (adapter in ADAPTERS) {
                if (adapter.isApplicable(context, classes)) {
                    return adapter.loadClasses(context, classes)
                }
            }

            // Should never happen because OrdinaryClassLoadingAdapter is always applicable
            throw IllegalStateException("Class loading adapter not found for $context")
        }
    }

    fun isApplicable(context: EvaluationContextImpl, classes: Collection<ClassToLoad>): Boolean

    fun loadClasses(context: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderHandler

    fun mirrorOfByteArray(bytes: ByteArray, context: EvaluationContextImpl, process: DebugProcessImpl): ArrayReference {
        val arrayClass = process.findClass(context, "byte[]", context.classLoader) as ArrayType
        val reference = process.newInstance(arrayClass, bytes.size)
        DebuggerUtilsEx.keep(reference, context)

        val mirrors = ArrayList<Value>(bytes.size)
        for (byte in bytes) {
            mirrors += process.virtualMachineProxy.mirrorOf(byte)
        }
        reference.values = mirrors

        return reference
    }
}