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

import com.intellij.debugger.engine.JVMNameUtil
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.isDexDebug

class AndroidOClassLoadingAdapter : AbstractAndroidClassLoadingAdapter() {
    override fun isApplicable(context: EvaluationContextImpl, info: ClassLoadingAdapter.Companion.ClassInfoForEvaluator) = with(info) {
        isCompilingEvaluatorPreferred && context.debugProcess.isDexDebug()
    }

    private fun resolveClassLoaderClass(context: EvaluationContextImpl): ClassType? {
        return try {
            context.debugProcess.tryLoadClass(
                context, "dalvik.system.InMemoryDexClassLoader", context.classLoader
            ) as? ClassType
        } catch (e: EvaluateException) {
            null
        }
    }

    override fun loadClasses(context: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderReference {
        val process = context.debugProcess
        val inMemoryClassLoaderClass = resolveClassLoaderClass(context) ?: error("InMemoryDexClassLoader class not found")
        val constructorMethod = inMemoryClassLoaderClass.concreteMethodByName(
            JVMNameUtil.CONSTRUCTOR_NAME, "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V"
        ) ?: error("Constructor method not found")

        val dexBytes = dex(context, classes) ?: error("Can't dex classes")
        val dexBytesMirror = mirrorOfByteArray(dexBytes, context, process)
        val dexByteBuffer = wrapToByteBuffer(dexBytesMirror, context, process)

        val newClassLoader = process.newInstance(
            context, inMemoryClassLoaderClass, constructorMethod,
            listOf(dexByteBuffer, context.classLoader)
        )

        DebuggerUtilsEx.keep(newClassLoader, context)

        return newClassLoader as ClassLoaderReference
    }
}