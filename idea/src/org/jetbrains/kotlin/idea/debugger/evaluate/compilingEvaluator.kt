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

import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.openapi.projectRoots.JdkVersionUtil
import com.intellij.openapi.util.SystemInfo
import com.sun.jdi.ClassLoaderReference
import org.jetbrains.kotlin.idea.debugger.evaluate.CompilingEvaluatorUtils

fun loadClasses(evaluationContext: EvaluationContextImpl, classes: Collection<Pair<String, ByteArray>>) {
    val process = evaluationContext.debugProcess

    val classLoader: ClassLoaderReference
    try {
        classLoader = CompilingEvaluatorUtils.getClassLoader(evaluationContext, process)
    }
    catch (e: Exception) {
        throw EvaluateException("Error creating evaluation class loader: " + e, e)
    }

    val version = (process.virtualMachineProxy).version()
    val sdkVersion = JdkVersionUtil.getVersion(version)

    if (!SystemInfo.isJavaVersionAtLeast(sdkVersion.description)) {
        throw EvaluateException("Unable to compile for target level " + sdkVersion.description + ". Need to run IDEA on java version at least " + sdkVersion.description + ", currently running on " + SystemInfo.JAVA_RUNTIME_VERSION)
    }

    try {
        defineClasses(classes, evaluationContext, process, classLoader)
    }
    catch (e: Exception) {
        throw EvaluateException("Error during classes definition " + e, e)
    }

    evaluationContext.classLoader = classLoader
}

private fun defineClasses(
        classes: Collection<Pair<String, ByteArray>>,
        context: EvaluationContext,
        process: DebugProcess,
        classLoader: ClassLoaderReference
) {
    val lambdaSuperclasses = LAMBDA_SUPERCLASSES.map { it.name to it.bytes }
    for ((className, bytes) in lambdaSuperclasses + classes) {
        CompilingEvaluatorUtils.defineClass(className, bytes, context, process, classLoader)
    }
}

// This list should contain all superclasses of lambda classes.
// The order is relevant here: if we load Lambda first instead, during the definition of Lambda the class loader will try
// to load its superclass. It will succeed, probably with the help of some parent class loader, and the subsequent attempt to define
// the patched version of that superclass will fail with LinkageError (cannot redefine class)
private val LAMBDA_SUPERCLASSES = listOf(
        ClassBytes("kotlin.jvm.internal.Lambda")
)

private class ClassBytes(val name: String) {
    val bytes: ByteArray by lazy {
        val inputStream = this::class.java.classLoader.getResourceAsStream(name.replace('.', '/') + ".class")
                          ?: throw EvaluateException("Couldn't find $name class in current class loader")

        inputStream.use {
            it.readBytes()
        }
    }
}
