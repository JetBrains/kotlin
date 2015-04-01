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

package org.jetbrains.kotlin.idea.debugger.evaluate.compilingEvaluator

import kotlin.properties.Delegates
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.DebugProcess
import com.sun.jdi.ClassLoaderReference
import com.intellij.openapi.projectRoots.JdkVersionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.idea.debugger.evaluate.CompilingEvaluatorUtils

public fun loadClasses(evaluationContext: EvaluationContextImpl, classes: Collection<Pair<String, ByteArray>>) {
    val process = evaluationContext.getDebugProcess()

    val classLoader: ClassLoaderReference
    try {
        classLoader = CompilingEvaluatorUtils.getClassLoader(evaluationContext, process)
    }
    catch (e: Exception) {
        throw EvaluateException("Error creating evaluation class loader: " + e, e)
    }

    val version = (process.getVirtualMachineProxy()).version()
    val sdkVersion = JdkVersionUtil.getVersion(version)

    if (!SystemInfo.isJavaVersionAtLeast(sdkVersion.getDescription())) {
        throw EvaluateException("Unable to compile for target level " + sdkVersion.getDescription() + ". Need to run IDEA on java version at least " + sdkVersion.getDescription() + ", currently running on " + SystemInfo.JAVA_RUNTIME_VERSION)
    }

    try {
        defineClasses(classes, evaluationContext, process, classLoader)
    }
    catch (e: Exception) {
        throw EvaluateException("Error during classes definition " + e, e)
    }

    evaluationContext.setClassLoader(classLoader)
}

private fun defineClasses(
        classes: Collection<Pair<String, ByteArray>>,
        context: EvaluationContext,
        process: DebugProcess,
        classLoader: ClassLoaderReference
) {
    CompilingEvaluatorUtils.defineClass(FunctionImplBytes.name, FunctionImplBytes.bytes, context, process, classLoader)

    for ((className, bytes) in classes) {
        CompilingEvaluatorUtils.defineClass(className, bytes, context, process, classLoader)
    }
}

private object FunctionImplBytes {
    val bytes: ByteArray by Delegates.lazy {
        val inputStream = this.javaClass.getClassLoader().getResourceAsStream("kotlin/jvm/internal/FunctionImpl.class")
        if (inputStream != null) {
            try {
                return@lazy inputStream.readBytes()
            }
            finally {
                inputStream.close()
            }
        }

        throw EvaluateException("Couldn't find kotlin.jvm.internal.FunctionImpl class in current classloader")
    }

    val name = "kotlin.jvm.internal.FunctionImpl"
}