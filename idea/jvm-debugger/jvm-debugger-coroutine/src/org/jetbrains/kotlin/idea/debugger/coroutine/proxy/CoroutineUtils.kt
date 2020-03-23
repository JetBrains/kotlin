/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutine.command.CoroutineBuilder
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode

fun Method.isInvokeSuspend(): Boolean =
    name() == "invokeSuspend" && signature() == "(Ljava/lang/Object;)Ljava/lang/Object;"

fun Method.isContinuation() =
    isInvokeSuspend() && declaringType().isContinuation() /* Perhaps need to check for "Lkotlin/coroutines/Continuation;)" in signature() ? */

fun Method.isSuspendLambda() =
    isInvokeSuspend() && declaringType().isSuspendLambda()

fun Method.isResumeWith() =
    name() == "resumeWith" && signature() == "(Ljava/lang/Object;)V" && (declaringType().isSuspendLambda() || declaringType().isContinuation())

fun Location.isPreFlight(): Boolean {
    val method = safeMethod() ?: return false
    return method.isSuspendLambda() || method.isContinuation()
}

fun ReferenceType.isContinuation() =
    isBaseContinuationImpl() || isSubtype("kotlin.coroutines.Continuation")

fun Type.isBaseContinuationImpl() =
    isSubtype("kotlin.coroutines.jvm.internal.BaseContinuationImpl")

fun Type.isAbstractCoroutine() =
    isSubtype("kotlinx.coroutines.AbstractCoroutine")

fun Type.isSubTypeOrSame(className: String) =
    name() == className || isSubtype(className)

fun ReferenceType.isSuspendLambda() =
    SUSPEND_LAMBDA_CLASSES.any { isSubtype(it) }

fun Location.isPreExitFrame() =
    safeMethod()?.isResumeWith() ?: false

fun StackFrameProxyImpl.variableValue(variableName: String): ObjectReference? {
    val continuationVariable = safeVisibleVariableByName(variableName) ?: return null
    return getValue(continuationVariable) as? ObjectReference ?: return null
}

fun StackFrameProxyImpl.completionVariableValue(): ObjectReference? =
    variableValue("completion")

fun StackFrameProxyImpl.completion1VariableValue(): ObjectReference? =
    variableValue("completion")

fun StackFrameProxyImpl.thisVariableValue(): ObjectReference? =
    this.thisObject()

private fun Method.isGetCOROUTINE_SUSPENDED() =
    signature() == "()Ljava/lang/Object;" && name() == "getCOROUTINE_SUSPENDED" && declaringType().name() == "kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsKt"

fun DefaultExecutionContext.findCoroutineMetadataType() =
    debugProcess.invokeInManagerThread { findClassSafe("kotlin.coroutines.jvm.internal.DebugMetadataKt") }

fun DefaultExecutionContext.findDispatchedContinuationReferenceType(): List<ReferenceType>? =
    vm.classesByName("kotlinx.coroutines.DispatchedContinuation")

fun DefaultExecutionContext.findCancellableContinuationImplReferenceType(): List<ReferenceType>? =
    vm.classesByName("kotlinx.coroutines.CancellableContinuationImpl")

fun hasGetCoroutineSuspended(frames: List<StackFrameProxyImpl>) =
    frames.indexOfFirst { it.safeLocation()?.safeMethod()?.isGetCOROUTINE_SUSPENDED() == true }

fun StackTraceElement.isCreationSeparatorFrame() =
    className.startsWith(CoroutineBuilder.CREATION_STACK_TRACE_SEPARATOR)

fun StackTraceElement.findPosition(project: Project): XSourcePosition? =
    getPosition(project, className, lineNumber)

fun Location.findPosition(project: Project) =
    getPosition(project, declaringType().name(), lineNumber())

fun ClassType.completionField() =
    fieldByName("completion")

private fun getPosition(project: Project, className: String, lineNumber: Int): XSourcePosition? {
    val psiFacade = JavaPsiFacade.getInstance(project)
    val psiClass = psiFacade.findClass(
        className.substringBefore("$"), // find outer class, for which psi exists TODO
        GlobalSearchScope.everythingScope(project)
    )
    val classFile = psiClass?.containingFile?.virtualFile
    // to convert to 0-based line number or '-1' to do not move
    val localLineNumber = if (lineNumber > 0) lineNumber - 1 else return null
    return XDebuggerUtil.getInstance().createPosition(classFile, localLineNumber)
}
/**
 * Finds previous Continuation for this Continuation (completion field in BaseContinuationImpl)
 * @return null if given ObjectReference is not a BaseContinuationImpl instance or completion is null
 */
fun getNextFrame(context: DefaultExecutionContext, continuation: ObjectReference): ObjectReference? {
    if (!continuation.type().isBaseContinuationImpl())
        return null
    val type = continuation.type() as ClassType
    val next = type.concreteMethodByName("getCompletion", "()Lkotlin/coroutines/Continuation;")
    return context.invokeMethod(continuation, next, emptyList()) as? ObjectReference
}

fun SuspendContextImpl.executionContext() =
    invokeInManagerThread { DefaultExecutionContext(EvaluationContextImpl(this, this.frameProxy)) }

fun <T : Any> SuspendContextImpl.invokeInManagerThread(f: () -> T?) : T? =
    debugProcess.invokeInManagerThread { f() }

fun ThreadReferenceProxyImpl.supportsEvaluation(): Boolean =
    threadReference?.isSuspended ?: false

fun SuspendContextImpl.supportsEvaluation() =
    this.debugProcess.canRunEvaluation || isUnitTestMode()

fun XDebugSession.suspendContextImpl() =
    suspendContext as SuspendContextImpl
