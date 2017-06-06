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
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.ClassLoadingUtils
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.openapi.projectRoots.JdkVersionUtil
import com.intellij.openapi.util.SystemInfo
import com.sun.jdi.ClassLoaderReference
import com.sun.jdi.ClassType
import org.jetbrains.kotlin.idea.debugger.evaluate.CompilingEvaluatorUtils
import org.jetbrains.kotlin.idea.debugger.isDexDebug

class OrdinaryClassLoadingAdapter : ClassLoadingAdapter {
    private companion object {
        // This list should contain all superclasses of lambda classes.
        // The order is relevant here: if we load Lambda first instead, during the definition of Lambda the class loader will try
        // to load its superclass. It will succeed, probably with the help of some parent class loader, and the subsequent attempt to define
        // the patched version of that superclass will fail with LinkageError (cannot redefine class)
        private val LAMBDA_SUPERCLASSES = listOf(ClassBytes("kotlin.jvm.internal.Lambda"))
    }

    override fun isApplicable(context: EvaluationContextImpl, hasAdditionalClasses: Boolean, hasLoops: Boolean): Boolean {
        return (hasAdditionalClasses || hasLoops) && context.classLoader != null && !context.debugProcess.isDexDebug()
    }

    override fun loadClasses(context: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderHandler {
        val process = context.debugProcess

        val classLoader = try {
            ClassLoadingUtils.getClassLoader(context, process)
        }
        catch (e: Exception) {
            throw EvaluateException("Error creating evaluation class loader: " + e, e)
        }

        val version = process.virtualMachineProxy.version()
        val sdkVersion = JdkVersionUtil.getVersion(version)

        if (!SystemInfo.isJavaVersionAtLeast(sdkVersion.description)) {
            throw EvaluateException(
                    "Unable to compile for target level ${sdkVersion.description}. " +
                    "Need to run IDEA on java version at least ${sdkVersion.description}, " +
                    "currently running on ${SystemInfo.JAVA_RUNTIME_VERSION}")
        }

        try {
            defineClasses(classes, context, process, classLoader)
        }
        catch (e: Exception) {
            throw EvaluateException("Error during classes definition " + e, e)
        }

        return ClassLoaderHandler(classLoader)
    }

    private fun defineClasses(
            classes: Collection<ClassToLoad>,
            context: EvaluationContextImpl,
            process: DebugProcessImpl,
            classLoader: ClassLoaderReference
    ) {
        val classesToLoad = if (classes.size == 1) {
            // No need in loading lambda superclass if there're no lambdas
            classes
        }
        else {
            val lambdaSuperclasses = LAMBDA_SUPERCLASSES.map {
                ClassToLoad(it.name, it.name.replace('.', '/') + ".class", it.bytes)
            }
            lambdaSuperclasses + classes
        }

        for ((className, _, bytes) in classesToLoad) {
            val patchedBytes = CompilingEvaluatorUtils.changeSuperToMagicAccessor(bytes)
            defineClass(className, patchedBytes, context, process, classLoader)
        }
    }

    fun defineClass(
            name: String,
            bytes: ByteArray,
            context: EvaluationContextImpl,
            process: DebugProcessImpl,
            classLoader: ClassLoaderReference
    ) {
        try {
            val vm = process.virtualMachineProxy
            val classLoaderType = classLoader.referenceType() as ClassType
            val defineMethod = classLoaderType.concreteMethodByName("defineClass", "(Ljava/lang/String;[BII)Ljava/lang/Class;")
            val nameObj = vm.mirrorOf(name)

            DebuggerUtilsEx.keep(nameObj, context)

            process.invokeMethod(
                    context, classLoader, defineMethod,
                    listOf(nameObj, mirrorOfByteArray(bytes, context, process), vm.mirrorOf(0), vm.mirrorOf(bytes.size)))
        }
        catch (e: Exception) {
            throw EvaluateException("Error during class $name definition: $e", e)
        }

    }

    private class ClassBytes(val name: String) {
        val bytes: ByteArray by lazy {
            val inputStream = this::class.java.classLoader.getResourceAsStream(name.replace('.', '/') + ".class")
                              ?: throw EvaluateException("Couldn't find $name class in current class loader")

            inputStream.use {
                it.readBytes()
            }
        }
    }
}