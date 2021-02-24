/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class JavaLangMirror(context: DefaultExecutionContext) {
    // java.lang.Object
    private val classClsRef = context.findClass("java.lang.Object") as ClassType
    val toString: Method = classClsRef.concreteMethodByName("toString", "()Ljava/lang/String;")

    // java.util.List
    private val listClsRef = context.findClass("java.util.List") as InterfaceType
    private val sizeRef: Method = listClsRef.methodsByName("size").single()
    private val getRef: Method = listClsRef.methodsByName("get").single()

    // java.lang.StackTraceElement
    private val stackTraceElementClsRef = context.findClass("java.lang.StackTraceElement") as ClassType
    private val methodNameFieldRef: Field = stackTraceElementClsRef.fieldByName("methodName")
    private val declaringClassFieldRef: Field = stackTraceElementClsRef.fieldByName("declaringClass")
    private val fileNameFieldRef: Field = stackTraceElementClsRef.fieldByName("fileName")
    private val lineNumberFieldRef: Field = stackTraceElementClsRef.fieldByName("lineNumber")

    fun string(state: ObjectReference, context: DefaultExecutionContext): String =
        (context.invokeMethod(state, toString, emptyList()) as StringReference).value()

    fun elementFromList(instance: ObjectReference, num: Int, context: DefaultExecutionContext) =
        context.invokeMethod(
            instance, getRef,
            listOf(context.vm.virtualMachine.mirrorOf(num))
        ) as ObjectReference

    fun sizeOf(args: ObjectReference, context: DefaultExecutionContext): Int =
        (context.invokeMethod(args, sizeRef, emptyList()) as IntegerValue).value()

    fun stackTraceElement(frame: ObjectReference) =
        StackTraceElement(
            fetchClassName(frame),
            fetchMethodName(frame),
            fetchFileName(frame),
            fetchLine(frame)
        )

    private fun fetchLine(instance: ObjectReference) =
        (instance.getValue(lineNumberFieldRef) as? IntegerValue)?.value() ?: -1

    private fun fetchFileName(instance: ObjectReference) =
        (instance.getValue(fileNameFieldRef) as? StringReference)?.value() ?: ""

    private fun fetchMethodName(instance: ObjectReference) =
        (instance.getValue(methodNameFieldRef) as? StringReference)?.value() ?: ""

    private fun fetchClassName(instance: ObjectReference) =
        (instance.getValue(declaringClassFieldRef) as? StringReference)?.value() ?: ""
}

class JavaUtilAbstractCollection(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfJavaLangAbstractCollection>("java.util.AbstractCollection", context) {
    private val abstractList = JavaUtilAbstractList(context)
    private val sizeMethod = makeMethod("size")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfJavaLangAbstractCollection? {
        val list = mutableListOf<ObjectReference>()
        val size = intValue(value, sizeMethod, context) ?: 0
        for (index in 0 until size) {
            val reference = abstractList.get(value, index, context) ?: continue
            list.add(reference)
        }
        return MirrorOfJavaLangAbstractCollection(value, list)
    }
}

class JavaUtilAbstractList(context: DefaultExecutionContext) :
    BaseMirror<ObjectReference>("java.util.AbstractList", context) {
    val getMethod = makeMethod("get")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): Nothing? =
        null

    fun get(value: ObjectReference, index: Int, context: DefaultExecutionContext): ObjectReference? =
        objectValue(value, getMethod, context, context.vm.mirrorOf(index))
}


data class MirrorOfJavaLangAbstractCollection(val that: ObjectReference, val values: List<ObjectReference>)

