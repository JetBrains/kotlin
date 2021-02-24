/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isSubTypeOrSame
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

abstract class BaseMirror<T>(val name: String, context: DefaultExecutionContext) : ReferenceTypeProvider {
    val log by logger
    private val cls = context.findClassSafe(name) ?: throw IllegalStateException("coroutine-debugger: class $name not found.")

    override fun getCls(): ClassType = cls

    fun makeField(fieldName: String): Field? =
        cls.fieldByName(fieldName)

    fun makeMethod(methodName: String): Method? =
        cls.methodsByName(methodName).singleOrNull()

    fun makeMethod(methodName: String, signature: String): Method? =
        cls.methodsByName(methodName, signature).singleOrNull()

    fun isCompatible(value: ObjectReference?) =
        value?.referenceType()?.isSubTypeOrSame(name) ?: false

    fun mirror(value: ObjectReference?, context: DefaultExecutionContext): T? {
        value ?: return null
        return if (!isCompatible(value)) {
            log.trace("Value ${value.referenceType()} is not compatible with $name.")
            null
        } else
            fetchMirror(value, context)
    }

    fun staticObjectValue(fieldName: String): ObjectReference? {
        val keyFieldRef = makeField(fieldName)
        return cls.let { it.getValue(keyFieldRef) as? ObjectReference }
    }

    fun staticMethodValue(instance: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value?) =
        instance?.let {
            method?.let { m ->
                context.invokeMethod(it, m, values.asList()) as? ObjectReference
            }
        }

    fun staticMethodValue(method: Method?, context: DefaultExecutionContext, vararg values: Value?) =
        cls.let {
            method?.let {
                context.invokeMethodSafe(cls, method, values.asList()) as? ObjectReference
            }
        }

    fun stringValue(value: ObjectReference, field: Field?) =
        field?.let {
            (value.getValue(it) as? StringReference)?.value()
        }

    fun byteValue(value: ObjectReference, field: Field?) =
        field?.let {
            (value.getValue(it) as? ByteValue)?.value()
        }

    fun threadValue(value: ObjectReference, field: Field?) =
        field?.let {
            value.getValue(it) as? ThreadReference
        }

    fun stringValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext) =
        method?.let {
            (context.invokeMethod(value, it, emptyList()) as? StringReference)?.value()
        }

    fun objectValue(value: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
        value?.let {
            method?.let {
                context.invokeMethodAsObject(value, method, *values)
            }
        }

    fun longValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
        method?.let { (context.invokeMethod(value, it, values.asList()) as? LongValue)?.longValue() }

    fun intValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
        method?.let { (context.invokeMethod(value, it, values.asList()) as? IntegerValue)?.intValue() }

    fun booleanValue(value: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value): Boolean? {
        value ?: return null
        method ?: return null
        return (context.invokeMethod(value, method, values.asList()) as? BooleanValue)?.booleanValue()
    }

    fun objectValue(value: ObjectReference, field: Field?) =
        field?.let { value.getValue(it) as ObjectReference? }

    fun intValue(value: ObjectReference, field: Field?) =
        field?.let { (value.getValue(it) as? IntegerValue)?.intValue() }

    fun longValue(value: ObjectReference, field: Field?) =
        field?.let { (value.getValue(it) as? LongValue)?.longValue() }

    fun booleanValue(value: ObjectReference?, field: Field?) =
        field?.let { (value?.getValue(field) as? BooleanValue)?.booleanValue() }

    protected abstract fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): T?
}

class StandaloneCoroutine private constructor(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfStandaloneCoroutine>("kotlinx.coroutines.StandaloneCoroutine", context) {
    private val coroutineContextMirror = CoroutineContext(context)
    private val childContinuationMirror = ChildContinuation(context)
    private val stateFieldRef = makeField("_state") // childContinuation
    private val contextFieldRef = makeField("context")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStandaloneCoroutine {
        val state = objectValue(value, stateFieldRef)
        val childContinuation = childContinuationMirror.mirror(state, context)
        val cc = objectValue(value, contextFieldRef)
        val coroutineContext = coroutineContextMirror.mirror(cc, context)
        return MirrorOfStandaloneCoroutine(value, childContinuation, coroutineContext)
    }

    companion object {
        val log by logger

        fun instance(context: DefaultExecutionContext): StandaloneCoroutine? {
            return try {
                StandaloneCoroutine(context)
            } catch (e: IllegalStateException) {
                log.debug("Attempt to access DebugProbesImpl but none found.", e)
                null
            }
        }
    }
}

data class MirrorOfStandaloneCoroutine(
    val that: ObjectReference,
    val state: MirrorOfChildContinuation?,
    val context: MirrorOfCoroutineContext?
)

class ChildContinuation(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfChildContinuation>("kotlinx.coroutines.ChildContinuation", context) {
    private val childContinuationMirror = CancellableContinuationImpl(context)
    private val childFieldRef = makeField("child") // cancellableContinuationImpl

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfChildContinuation? {
        val child = objectValue(value, childFieldRef)
        return MirrorOfChildContinuation(value, childContinuationMirror.mirror(child, context))
    }
}

data class MirrorOfChildContinuation(
    val that: ObjectReference,
    val child: MirrorOfCancellableContinuationImpl?
)

class CancellableContinuationImpl(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfCancellableContinuationImpl>("kotlinx.coroutines.CancellableContinuationImpl", context) {
    private val coroutineContextMirror = CoroutineContext(context)
    private val dispatchedContinuationMirror = DispatchedContinuation(context)
    private val decisionFieldRef = makeField("_decision")
    private val delegateFieldRef = makeField("delegate") // DispatchedContinuation
    private val resumeModeFieldRef = makeField("resumeMode")
    private val submissionTimeFieldRef = makeField("submissionTime")
    private val contextFieldRef = makeField("context")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCancellableContinuationImpl? {
        val decision = intValue(value, decisionFieldRef)
        val dispatchedContinuation = dispatchedContinuationMirror.mirror(objectValue(value, delegateFieldRef), context)
        val submissionTime = longValue(value, submissionTimeFieldRef)
        val resumeMode = intValue(value, resumeModeFieldRef)
        val coroutineContext = objectValue(value, contextFieldRef)
        val contextMirror = coroutineContextMirror.mirror(coroutineContext, context)
        return MirrorOfCancellableContinuationImpl(value, decision, dispatchedContinuation, resumeMode, submissionTime, contextMirror)
    }
}

data class MirrorOfCancellableContinuationImpl(
    val that: ObjectReference,
    val decision: Int?,
    val delegate: MirrorOfDispatchedContinuation?,
    val resumeMode: Int?,
    val submissionTime: Long?,
    val jobContext: MirrorOfCoroutineContext?
)

class DispatchedContinuation(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfDispatchedContinuation>("kotlinx.coroutines.DispatchedContinuation", context) {
    private val decisionFieldRef = makeField("continuation")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDispatchedContinuation? {
        val continuation = objectValue(value, decisionFieldRef)
        return MirrorOfDispatchedContinuation(value, continuation)
    }
}

data class MirrorOfDispatchedContinuation(
    val that: ObjectReference,
    val continuation: ObjectReference?,
)
