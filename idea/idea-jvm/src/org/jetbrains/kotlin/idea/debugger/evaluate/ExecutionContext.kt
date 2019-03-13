/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.openapi.project.Project
import com.sun.jdi.*
import org.jetbrains.org.objectweb.asm.Type

class ExecutionContext(val evaluationContext: EvaluationContextImpl, val frameProxy: StackFrameProxyImpl) {
    val vm: VirtualMachineProxyImpl
        get() = evaluationContext.debugProcess.virtualMachineProxy

    val classLoader: ClassLoaderReference?
        get() = evaluationContext.classLoader

    val suspendContext: SuspendContextImpl
        get() = evaluationContext.suspendContext

    val debugProcess: DebugProcessImpl
        get() = evaluationContext.debugProcess

    val project: Project
        get() = evaluationContext.project

    val invokePolicy = evaluationContext.suspendContext.getInvokePolicy()

    @Throws(EvaluateException::class)
    fun invokeMethod(obj: ObjectReference, method: Method, args: List<Value?>): Value? {
        return debugProcess.invokeInstanceMethod(evaluationContext, obj, method, args, invokePolicy)
    }

    fun invokeMethod(type: ClassType, method: Method, args: List<Value?>): Value? {
        return debugProcess.invokeMethod(evaluationContext, type, method, args)
    }

    @Throws(EvaluateException::class)
    fun newInstance(type: ClassType, constructor: Method, args: List<Value?>): ObjectReference {
        return debugProcess.newInstance(evaluationContext, type, constructor, args)
    }

    @Throws(EvaluateException::class)
    fun newInstance(arrayType: ArrayType, dimension: Int): ArrayReference {
        return debugProcess.newInstance(arrayType, dimension)
    }

    @Throws(EvaluateException::class)
    fun findClass(name: String, classLoader: ClassLoaderReference? = null): ReferenceType? {
        debugProcess.findClass(evaluationContext, name, classLoader)?.let { return it }

        // If 'isAutoLoadClasses' is true, `findClass()` already did this
        if (!evaluationContext.isAutoLoadClasses) {
            try {
                debugProcess.loadClass(evaluationContext, name, classLoader)
            } catch (e: InvocationException) {
                throw EvaluateExceptionUtil.createEvaluateException(e)
            } catch (e: ClassNotLoadedException) {
                throw EvaluateExceptionUtil.createEvaluateException(e)
            } catch (e: IncompatibleThreadStateException) {
                throw EvaluateExceptionUtil.createEvaluateException(e)
            } catch (e: InvalidTypeException) {
                throw EvaluateExceptionUtil.createEvaluateException(e)
            }
        }

        return null
    }

    @Throws(EvaluateException::class)
    fun findClass(asmType: Type, classLoader: ClassLoaderReference? = null): ReferenceType? {
        if (asmType.sort != Type.OBJECT && asmType.sort != Type.ARRAY) {
            return null
        }

        return findClass(asmType.className, classLoader)
    }

    fun keepReference(reference: ObjectReference) {
        // Not available in older IDEA versions
        @Suppress("DEPRECATION")
        DebuggerUtilsEx.keep(reference, evaluationContext)
    }
}