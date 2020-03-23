/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class DebugProbesImpl(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfDebugProbesImpl>("kotlinx.coroutines.debug.internal.DebugProbesImpl", context) {
    val javaLangListMirror = JavaUtilList(context)
    val stackTraceElement = StackTraceElement(context)
    val coroutineInfo = CoroutineInfo(this, context)
    val instance = staticObjectValue("INSTANCE")
    val isInstalledMethod = makeMethod("isInstalled\$kotlinx_coroutines_debug", "()Z")
    val isInstalledValue = booleanValue(instance, isInstalledMethod, context)
    val enhanceStackTraceWithThreadDumpMethod = makeMethod("enhanceStackTraceWithThreadDump")
    val dumpMethod = makeMethod("dumpCoroutinesInfo", "()Ljava/util/List;")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDebugProbesImpl? {
        return MirrorOfDebugProbesImpl(value, instance, isInstalledValue)
    }

    fun enchanceStackTraceWithThreadDump(
        context: DefaultExecutionContext,
        coroutineInfo: ObjectReference,
        lastObservedStackTrace: ObjectReference
    ): List<MirrorOfStackTraceElement>? {
        val listReference =
            staticMethodValue(instance, enhanceStackTraceWithThreadDumpMethod, context, coroutineInfo, lastObservedStackTrace)
        val list = javaLangListMirror.mirror(listReference, context) ?: return null
        return list.values.mapNotNull { stackTraceElement.mirror(it, context) }
    }

    fun dumpCoroutinesInfo(context: DefaultExecutionContext): List<MirrorOfCoroutineInfo> {
        instance ?: return emptyList()
        val coroutinesInfoReference = objectValue(instance, dumpMethod, context)
        val referenceList = javaLangListMirror.mirror(coroutinesInfoReference, context) ?: return emptyList()
        return referenceList.values.mapNotNull { coroutineInfo.mirror(it, context) }
    }

    fun getCoroutineInfo(input: ObjectReference?, context: DefaultExecutionContext): MirrorOfCoroutineInfo? {
        // kotlinx.coroutines.debug.internal.DebugProbesImpl$CoroutineOwner
        val delegate = input?.referenceType()?.fieldByName("info") ?: return null
        val coroutine = input.getValue(delegate) as? ObjectReference
        return coroutineInfo.mirror(coroutine, context)
    }
}

data class MirrorOfDebugProbesImpl(val that: ObjectReference, val instance: ObjectReference?, val isInstalled: Boolean?)

class CoroutineInfo(val debugProbesImplMirror: DebugProbesImpl, context: DefaultExecutionContext) :
    BaseMirror<MirrorOfCoroutineInfo>("kotlinx.coroutines.debug.CoroutineInfo", context) {
    val javaLangMirror = JavaLangMirror(context)
    val javaLangListMirror = JavaUtilList(context)
    private val coroutineContextMirror = CoroutineContext(context)
    private val coroutineStackFrameMirror = CoroutineStackFrame(context)
    private val stackTraceElement = StackTraceElement(context)
    private val contextFieldRef = makeField("context")
    private val creationStackBottom = makeField("creationStackBottom")
    private val sequenceNumberField = makeField("sequenceNumber")
    private val creationStackTraceMethod = makeMethod("getCreationStackTrace")
    private val stateMethod = makeMethod("getState")
    private val lastObservedStackTraceMethod = makeMethod("lastObservedStackTrace")

    private val lastObservedFrameField = makeField("lastObservedFrame")
    private val lastObservedThreadField = makeField("lastObservedThread")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineInfo {
        val state = objectValue(value, stateMethod, context)?.let {
            stringValue(it, javaLangMirror.toString, context)
        }
        val coroutineContext = coroutineContextMirror.mirror(objectValue(value, contextFieldRef), context)
        val creationStackBottomObjectReference = objectValue(value, creationStackBottom)
        val creationStackBottom = coroutineStackFrameMirror.mirror(creationStackBottomObjectReference, context)
        val sequenceNumber = longValue(value, sequenceNumberField)
        val creationStackTraceList = objectValue(value, creationStackTraceMethod, context)
        val creationStackTraceMirror = javaLangListMirror.mirror(creationStackTraceList, context)
        val creationStackTrace = creationStackTraceMirror?.values?.mapNotNull { stackTraceElement.mirror(it, context) }

        val lastObservedStackTrace = objectValue(value, lastObservedStackTraceMethod, context)
        val enchancedList =
            if (lastObservedStackTrace != null)
                debugProbesImplMirror.enchanceStackTraceWithThreadDump(context, value, lastObservedStackTrace)
            else emptyList()
        val lastObservedThread = threadValue(value, lastObservedThreadField)
        val lastObservedFrame = threadValue(value, lastObservedFrameField)
        return MirrorOfCoroutineInfo(
            value,
            coroutineContext,
            creationStackBottom,
            sequenceNumber,
            enchancedList,
            creationStackTrace,
            state,
            lastObservedThread,
            lastObservedFrame
        )
    }

}

data class MirrorOfCoroutineInfo(
    val that: ObjectReference,
    val context: MirrorOfCoroutineContext?,
    val creationStackBottom: MirrorOfCoroutineStackFrame?,
    val sequenceNumber: Long?,
    val enchancedStackTrace: List<MirrorOfStackTraceElement>?,
    val creationStackTrace: List<MirrorOfStackTraceElement>?,
    val state: String?,
    val lastObservedThread: ThreadReference?,
    val lastObservedFrame: ObjectReference?
)

class CoroutineStackFrame(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfCoroutineStackFrame>("kotlin.coroutines.jvm.internal.CoroutineStackFrame", context) {
    private val stackTraceElementMirror = StackTraceElement(context)
    private val callerFrameMethod = makeMethod("getCallerFrame")
    private val getStackTraceElementMethod = makeMethod("getStackTraceElement")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineStackFrame? {
        val objectReference = objectValue(value, callerFrameMethod, context)
        val callerFrame = if (objectReference is ObjectReference)
            this.mirror(objectReference, context) else null
        val stackTraceElementReference = objectValue(value, getStackTraceElementMethod, context)
        val stackTraceElement =
            if (stackTraceElementReference is ObjectReference) stackTraceElementMirror.mirror(stackTraceElementReference, context) else null
        return MirrorOfCoroutineStackFrame(value, callerFrame, stackTraceElement)
    }
}

data class MirrorOfCoroutineStackFrame(
    val that: ObjectReference,
    val callerFrame: MirrorOfCoroutineStackFrame?,
    val stackTraceElement: MirrorOfStackTraceElement?
)

class StackTraceElement(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfStackTraceElement>("java.lang.StackTraceElement", context) {
    private val declaringClassObjectField = makeField("declaringClass")
    private val moduleNameField = makeField("moduleName")
    private val moduleVersionField = makeField("moduleVersion")
    private val declaringClassField = makeField("declaringClass")
    private val methodNameField = makeField("methodName")
    private val fileNameField = makeField("fileName")
    private val lineNumberField = makeField("lineNumber")
    private val formatField = makeField("format")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStackTraceElement? {
        val declaringClassObject = objectValue(value, declaringClassObjectField)
        val moduleName = stringValue(value, moduleNameField)
        val moduleVersion = stringValue(value, moduleVersionField)
        val declaringClass = stringValue(value, declaringClassField)
        val methodName = stringValue(value, methodNameField)
        val fileName = stringValue(value, fileNameField)
        val lineNumber = intValue(value, lineNumberField)
        val format = byteValue(value, formatField)
        return MirrorOfStackTraceElement(
            value,
            declaringClassObject,
            moduleName,
            moduleVersion,
            declaringClass,
            methodName,
            fileName,
            lineNumber,
            format
        )
    }
}

data class MirrorOfStackTraceElement(
    val that: ObjectReference,
    val declaringClassObject: ObjectReference?,
    val moduleName: String?,
    val moduleVersion: String?,
    val declaringClass: String?,
    val methodName: String?,
    val fileName: String?,
    val lineNumber: Int?,
    val format: Byte?
) {
    fun stackTraceElement() =
        java.lang.StackTraceElement(
            declaringClass,
            methodName,
            fileName,
            lineNumber ?: -1
        )
}