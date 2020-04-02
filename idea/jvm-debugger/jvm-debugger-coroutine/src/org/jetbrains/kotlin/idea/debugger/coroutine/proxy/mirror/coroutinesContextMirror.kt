/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.Method
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class CoroutineContext(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfCoroutineContext>("kotlin.coroutines.CoroutineContext", context) {
    val coroutineNameRef = CoroutineName(context)
    val coroutineIdRef = CoroutineId(context)
    val jobRef = Job(context)
    val getContextElement: Method = makeMethod("get")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCoroutineContext? {
        val coroutineName = getElementValue(value, context, coroutineNameRef) ?: "coroutine"
        val coroutineId = getElementValue(value, context, coroutineIdRef)
        val job = getElementValue(value, context, jobRef)
        return MirrorOfCoroutineContext(value, coroutineName, coroutineId, job)
    }

    fun <T> getElementValue(value: ObjectReference, context: DefaultExecutionContext, keyProvider: ContextKey<T>): T? {
        val elementValue = objectValue(value, getContextElement, context, keyProvider.key()) ?: return null
        return keyProvider.mirror(elementValue, context)
    }
}

data class MirrorOfCoroutineContext(
    val that: ObjectReference,
    val name: String,
    val id: Long?,
    val job: ObjectReference?
)

abstract class ContextKey<T>(name: String, context: DefaultExecutionContext) : BaseMirror<T>(name, context) {
    abstract fun key() : ObjectReference
}

class CoroutineName(context: DefaultExecutionContext) : ContextKey<String>("kotlinx.coroutines.CoroutineName", context) {
    val key = staticObjectValue("Key")
    val getNameRef: Method = makeMethod("getName")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): String? {
        return stringValue(value, getNameRef, context)
    }

    override fun key() = key
}

class CoroutineId(context: DefaultExecutionContext) : ContextKey<Long>("kotlinx.coroutines.CoroutineId", context) {
    val key = staticObjectValue("Key")
    val getIdRef: Method = makeMethod("getId")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): Long? {
        return longValue(value, getIdRef, context)
    }

    override fun key() = key
}

class Job(context: DefaultExecutionContext) : ContextKey<ObjectReference>("kotlinx.coroutines.Job", context) {
    val key = staticObjectValue("Key")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): ObjectReference? {
        return value
    }

    override fun key() = key
}