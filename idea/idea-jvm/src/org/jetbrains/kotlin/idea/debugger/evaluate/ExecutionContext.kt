/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.sun.jdi.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.org.objectweb.asm.Type

class ExecutionContext(val evaluationContext: EvaluationContextImpl, val thread: ThreadReference, val invokePolicy: Int) {
    val vm: VirtualMachine
        get() = thread.virtualMachine()

    fun loadClassType(asmType: Type, classLoader: ClassLoaderReference? = null): ReferenceType? {
        if (asmType.sort == Type.ARRAY) {
            return loadClassType(asmType.elementType, classLoader)
        }

        if (asmType.sort != Type.OBJECT) {
            return null
        }

        val vm = thread.virtualMachine()
        val className = asmType.className

        val classClass = vm.classesByName(Class::class.java.name).firstIsInstanceOrNull<ClassType>() ?: return null

        val method: Method?
        val args: List<Value?>

        if (classLoader != null) {
            method = classClass.methodsByName("forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;").firstOrNull()
            args = listOf(vm.mirrorOf(className), vm.mirrorOf(true), classLoader)
        } else {
            method = classClass.methodsByName("forName", "(Ljava/lang/String;)Ljava/lang/Class;").firstOrNull()
            args = listOf(vm.mirrorOf(className))
        }

        if (method == null) {
            return null
        }

        return (classClass.invokeMethod(thread, method, args, invokePolicy) as? ClassObjectReference)?.reflectedType()
    }

}