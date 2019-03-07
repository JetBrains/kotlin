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

import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.impl.ClassLoadingUtils
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.sun.jdi.ClassLoaderReference
import com.sun.jdi.ClassType
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.isDexDebug
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes

class OrdinaryClassLoadingAdapter : ClassLoadingAdapter {
    private companion object {
        // This list should contain all superclasses of lambda classes.
        // The order is relevant here: if we load Lambda first instead, during the definition of Lambda the class loader will try
        // to load its superclass. It will succeed, probably with the help of some parent class loader, and the subsequent attempt to define
        // the patched version of that superclass will fail with LinkageError (cannot redefine class)
        private val LAMBDA_SUPERCLASSES = listOf(ClassBytes("kotlin.jvm.internal.Lambda"))

        // Copied from com.intellij.debugger.ui.impl.watch.CompilingEvaluator.changeSuperToMagicAccessor
        fun changeSuperToMagicAccessor(bytes: ByteArray): ByteArray {
            val classWriter = ClassWriter(0)
            val classVisitor = object : ClassVisitor(Opcodes.API_VERSION, classWriter) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superName: String?,
                    interfaces: Array<String>?
                ) {
                    var newSuperName = superName
                    if ("java/lang/Object" == newSuperName) {
                        newSuperName = "sun/reflect/MagicAccessorImpl"
                    }

                    super.visit(version, access, name, signature, newSuperName, interfaces)
                }
            }
            ClassReader(bytes).accept(classVisitor, 0)
            return classWriter.toByteArray()
        }

        fun useMagicAccessor(context: ExecutionContext): Boolean {
            val rawVersion = context.vm.version()?.substringBefore('_') ?: return false
            val javaVersion = JavaSdkVersion.fromVersionString(rawVersion) ?: return false
            return !javaVersion.isAtLeast(JavaSdkVersion.JDK_1_9)
        }
    }

    override fun isApplicable(context: ExecutionContext, info: ClassLoadingAdapter.Companion.ClassInfoForEvaluator): Boolean {
        return info.isCompilingEvaluatorPreferred && context.classLoader != null && !context.debugProcess.isDexDebug()
    }

    override fun loadClasses(context: ExecutionContext, classes: Collection<ClassToLoad>): ClassLoaderReference {
        val process = context.debugProcess

        val classLoader = try {
            ClassLoadingUtils.getClassLoader(context.evaluationContext, process)
        } catch (e: Exception) {
            throw EvaluateException("Error creating evaluation class loader: $e", e)
        }

        try {
            defineClasses(classes, context, classLoader)
        } catch (e: Exception) {
            throw EvaluateException("Error during classes definition $e", e)
        }

        return classLoader
    }

    private fun defineClasses(
        classes: Collection<ClassToLoad>,
        context: ExecutionContext,
        classLoader: ClassLoaderReference
    ) {
        val classesToLoad = if (classes.size == 1) {
            // No need in loading lambda superclass if there're no lambdas
            classes
        } else {
            val lambdaSuperclasses = LAMBDA_SUPERCLASSES.map {
                ClassToLoad(it.name, it.name.replace('.', '/') + ".class", it.bytes)
            }
            lambdaSuperclasses + classes
        }

        for ((className, _, bytes) in classesToLoad) {
            val patchedBytes = if (useMagicAccessor(context)) changeSuperToMagicAccessor(bytes) else bytes
            defineClass(className, patchedBytes, context, classLoader)
        }
    }

    private fun defineClass(
        name: String,
        bytes: ByteArray,
        context: ExecutionContext,
        classLoader: ClassLoaderReference
    ) {
        try {
            val vm = context.vm
            val classLoaderType = classLoader.referenceType() as ClassType
            val defineMethod = classLoaderType.concreteMethodByName("defineClass", "(Ljava/lang/String;[BII)Ljava/lang/Class;")
            val nameObj = vm.mirrorOf(name)

            val args = listOf(nameObj, mirrorOfByteArray(bytes, context), vm.mirrorOf(0), vm.mirrorOf(bytes.size))
            context.invokeMethod(classLoader, defineMethod, args)
        } catch (e: Exception) {
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