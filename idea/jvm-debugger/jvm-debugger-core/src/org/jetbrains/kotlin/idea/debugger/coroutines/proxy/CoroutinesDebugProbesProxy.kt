/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.coroutines.proxy

import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.PrioritizedTask
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.xdebugger.XDebugProcess
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutines.command.CoroutineBuilder
import org.jetbrains.kotlin.idea.debugger.coroutines.view.CoroutineInfoCache
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

fun SuspendContextImpl.createEvaluationContext() =
    EvaluationContextImpl(this, this.frameProxy)

class CoroutinesDebugProbesProxy(val suspendContext: SuspendContextImpl) {
    private val log by logger

    // @TODO refactor to extract initialization logic
    private var executionContext: ExecutionContext = ExecutionContext(
        EvaluationContextImpl(suspendContext, suspendContext.frameProxy),
        suspendContext.frameProxy as StackFrameProxyImpl)
    // might want to use inner class but also having to monitor order of fields
    private var refs: ProcessReferences = ProcessReferences(executionContext)

    companion object {
        private const val DEBUG_PACKAGE = "kotlinx.coroutines.debug"
    }

    @Synchronized
    @Suppress("unused")
    fun install() =
        executionContext.invokeMethodAsVoid(refs.instance, "install")

    @Synchronized
    @Suppress("unused")
    fun uninstall() =
        executionContext.invokeMethodAsVoid(refs.instance, "uninstall")

    /**
     * Invokes DebugProbes from debugged process's classpath and returns states of coroutines
     * Should be invoked on debugger manager thread
     */
    @Synchronized
    fun dumpCoroutines() : CoroutineInfoCache {
        val coroutineInfoCache = CoroutineInfoCache()
        try {
            val infoList = dump()
            coroutineInfoCache.ok(infoList)
        } catch (e: Throwable) {
            log.error("Exception is thrown by calling dumpCoroutines.", e)
            coroutineInfoCache.fail()
        }
        return coroutineInfoCache
    }

    private fun dump(): List<CoroutineInfoData> {
        val coroutinesInfo = dumpCoroutinesInfo() ?: return emptyList()

        executionContext.keepReference(coroutinesInfo)
        val size = sizeOf(coroutinesInfo)

        return MutableList(size) {
            val elem = getElementFromList(coroutinesInfo, it)
            fetchCoroutineState(elem)
        }
    }

    private fun dumpCoroutinesInfo() =
        executionContext.invokeMethodAsObject(refs.instance, refs.dumpMethod)

    fun frameBuilder() = CoroutineBuilder(suspendContext)

    private fun getElementFromList(instance: ObjectReference, num: Int) =
        executionContext.invokeMethod(
            instance, refs.getRef,
            listOf(executionContext.vm.virtualMachine.mirrorOf(num))
        ) as ObjectReference

    private fun fetchCoroutineState(instance: ObjectReference) : CoroutineInfoData {
        val name = getName(instance)
        val state = getState(instance)
        val thread = getLastObservedThread(instance, refs.lastObservedThreadFieldRef)
        val lastObservedFrameFieldRef = instance.getValue(refs.lastObservedFrameFieldRef) as? ObjectReference
        return CoroutineInfoData(
            name,
            CoroutineInfoData.State.valueOf(state),
            getThreadName(instance),
            getThreadState(instance),

            getStackTrace(instance),
            thread,
            lastObservedFrameFieldRef
        )
    }

    private fun getThreadName(instance: ObjectReference) : String {
        return "thread name"
    }

    private fun getThreadState(instance: ObjectReference) : Int {
        return 1
    }

    private fun getName(
        info: ObjectReference // CoroutineInfo instance
    ): String {
        // equals to `coroutineInfo.context.get(CoroutineName).name`
        val coroutineContextInst = executionContext.invokeMethod(
            info,
            refs.getContextRef,
            emptyList()
        ) as? ObjectReference ?: throw IllegalArgumentException("Coroutine context must not be null")
        val coroutineName = executionContext.invokeMethod(
            coroutineContextInst,
            refs.getContextElement, listOf(refs.keyFieldValueRef)
        ) as? ObjectReference
        // If the coroutine doesn't have a given name, CoroutineContext.get(CoroutineName) returns null
        val name = if (coroutineName != null) (executionContext.invokeMethod(
            coroutineName,
            refs.getNameRef,
            emptyList()
        ) as StringReference).value() else "coroutine"
        val id = (info.getValue(refs.sequenceNumberFieldRef) as LongValue).value()
        return "$name#$id"
    }

    private fun getState(
        info: ObjectReference // CoroutineInfo instance
    ): String {
        // equals to `stringState = coroutineInfo.state.toString()`
        val state = executionContext.invokeMethod(info, refs.getStateRef, emptyList()) as ObjectReference
        return (executionContext.invokeMethod(state, refs.toString, emptyList()) as StringReference).value()
    }

    private fun getLastObservedThread(
        info: ObjectReference, // CoroutineInfo instance
        threadRef: Field // reference to lastObservedThread
    ): ThreadReference? = info.getValue(threadRef) as? ThreadReference

    /**
     * Returns list of stackTraceElements for the given CoroutineInfo's [ObjectReference]
     */
    private fun getStackTrace(
        info: ObjectReference
    ): List<StackTraceElement> {
        val frameList = lastObservedStackTrace(info)
        val tmpList = mutableListOf<StackTraceElement>()
        for(it in 0 until sizeOf(frameList)) {
            val frame = getElementFromList(frameList, it)
            val ste = newStackTraceElement(frame)
            tmpList.add(ste)
        }
        val mergedFrameList = enhanceStackTraceWithThreadDump(listOf(info, frameList))
        val size = sizeOf(mergedFrameList)

        val list = mutableListOf<StackTraceElement>()

        for (it in 0 until size) {
            val frame = getElementFromList(mergedFrameList, it)
            val ste = newStackTraceElement(frame)
            list.add(// 0, // add in the beginning // @TODO what's the point?
                ste)
        }
        return list
    }

    private fun newStackTraceElement(frame: ObjectReference) =
        StackTraceElement(
            fetchClassName(frame),
            fetchMethodName(frame),
            fetchFileName(frame),
            fetchLine(frame)
        )

    private fun fetchLine(instance: ObjectReference) =
        (instance.getValue(refs.lineNumberFieldRef) as? IntegerValue)?.value() ?: -1

    private fun fetchFileName(instance: ObjectReference) =
        (instance.getValue(refs.fileNameFieldRef) as? StringReference)?.value() ?: ""

    private fun fetchMethodName(instance: ObjectReference) =
        (instance.getValue(refs.methodNameFieldRef) as? StringReference)?.value() ?: ""

    private fun fetchClassName(instance: ObjectReference) =
        (instance.getValue(refs.declaringClassFieldRef) as? StringReference)?.value() ?: ""

    private fun lastObservedStackTrace(instance: ObjectReference) =
        executionContext.invokeMethod(instance, refs.lastObservedStackTraceRef, emptyList()) as ObjectReference

    private fun enhanceStackTraceWithThreadDump(args: List<ObjectReference>) =
        executionContext.invokeMethod(
            refs.debugProbesImplInstance,
            refs.enhanceStackTraceWithThreadDumpRef, args) as ObjectReference

    private fun sizeOf(args: ObjectReference): Int =
        (executionContext.invokeMethod(args, refs.sizeRef, emptyList()) as IntegerValue).value()

    /**
     * @TODO refactor later
     * Holds ClassTypes, Methods, ObjectReferences and Fields for a particular jvm
     */
    class ProcessReferences(executionContext: ExecutionContext) {
        // kotlinx.coroutines.debug.DebugProbes instance and methods
        val debugProbesClsRef = executionContext.findClass("$DEBUG_PACKAGE.DebugProbes") as ClassType
        val debugProbesImplClsRef = executionContext.findClass("$DEBUG_PACKAGE.internal.DebugProbesImpl") as ClassType
        val coroutineNameClsRef = executionContext.findClass("kotlinx.coroutines.CoroutineName") as ClassType
        val classClsRef = executionContext.findClass("java.lang.Object") as ClassType
        val debugProbesImplInstance = with(debugProbesImplClsRef) { getValue(fieldByName("INSTANCE")) as ObjectReference }
        val enhanceStackTraceWithThreadDumpRef: Method = debugProbesImplClsRef
            .methodsByName("enhanceStackTraceWithThreadDump").single()

        val dumpMethod: Method = debugProbesClsRef.concreteMethodByName("dumpCoroutinesInfo", "()Ljava/util/List;")
        val instance = with(debugProbesClsRef) { getValue(fieldByName("INSTANCE")) as ObjectReference }

        // CoroutineInfo
        val coroutineInfoClsRef = executionContext.findClass("$DEBUG_PACKAGE.CoroutineInfo") as ClassType
        val coroutineContextClsRef = executionContext.findClass("kotlin.coroutines.CoroutineContext") as InterfaceType

        val getStateRef: Method = coroutineInfoClsRef.concreteMethodByName("getState", "()Lkotlinx/coroutines/debug/State;")
        val getContextRef: Method = coroutineInfoClsRef.concreteMethodByName("getContext", "()Lkotlin/coroutines/CoroutineContext;")
        val sequenceNumberFieldRef: Field = coroutineInfoClsRef.fieldByName("sequenceNumber")
        val lastObservedStackTraceRef: Method = coroutineInfoClsRef.methodsByName("lastObservedStackTrace").single()
        val getContextElement: Method = coroutineContextClsRef.methodsByName("get").single()
        val getNameRef: Method = coroutineNameClsRef.methodsByName("getName").single()
        val keyFieldRef = coroutineNameClsRef.fieldByName("Key")
        val toString: Method = classClsRef.concreteMethodByName("toString", "()Ljava/lang/String;")

        val lastObservedThreadFieldRef: Field = coroutineInfoClsRef.fieldByName("lastObservedThread")
        val lastObservedFrameFieldRef: Field = coroutineInfoClsRef.fieldByName("lastObservedFrame") // continuation

        // Methods for list
        val listClsRef = executionContext.findClass("java.util.List") as InterfaceType
        val sizeRef: Method = listClsRef.methodsByName("size").single()
        val getRef: Method = listClsRef.methodsByName("get").single()
        val stackTraceElementClsRef = executionContext.findClass("java.lang.StackTraceElement") as ClassType

        // for StackTraceElement
        val methodNameFieldRef: Field = stackTraceElementClsRef.fieldByName("methodName")
        val declaringClassFieldRef: Field = stackTraceElementClsRef.fieldByName("declaringClass")
        val fileNameFieldRef: Field = stackTraceElementClsRef.fieldByName("fileName")
        val lineNumberFieldRef: Field = stackTraceElementClsRef.fieldByName("lineNumber")

        // value
        val keyFieldValueRef = coroutineNameClsRef.getValue(keyFieldRef) as ObjectReference

    }
}