/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.psi.PsiElement
import com.sun.jdi.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.asmTypeForAnonymousClass
import org.jetbrains.kotlin.codegen.coroutines.DO_RESUME_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.continuationAsmTypes
import org.jetbrains.kotlin.codegen.inline.KOTLIN_STRATA_NAME
import org.jetbrains.kotlin.idea.core.KotlinFileTypeFactory
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.util.getLineEndOffset
import org.jetbrains.kotlin.idea.core.util.getLineStartOffset
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import java.util.*

fun Location.isInKotlinSources(): Boolean {
    return declaringType().isInKotlinSources()
}

fun ReferenceType.isInKotlinSources(): Boolean {
    val fileExtension = safeSourceName()?.substringAfterLast('.')?.toLowerCase() ?: ""
    return fileExtension in KotlinFileTypeFactory.KOTLIN_EXTENSIONS || containsKotlinStrata()
}

fun ReferenceType.containsKotlinStrata() = availableStrata().contains(KOTLIN_STRATA_NAME)

fun isInsideInlineArgument(
    inlineArgument: KtFunction,
    location: Location,
    debugProcess: DebugProcessImpl,
    bindingContext: BindingContext = KotlinDebuggerCaches.getOrCreateTypeMapper(inlineArgument).bindingContext
): Boolean {
    val visibleVariables = location.visibleVariables(debugProcess)
    val markerLocalVariables = visibleVariables.filter { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT) }

    val context = KotlinDebuggerCaches.getOrCreateTypeMapper(inlineArgument).bindingContext
    val lambdaOrdinal = runReadAction { lambdaOrdinalByArgument(inlineArgument, context) }
    val functionName = runReadAction { functionNameByArgument(inlineArgument, context) }

    return markerLocalVariables
        .map { it.name().drop(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT.length) }
        .any { variableName ->
            if (variableName.startsWith("-")) {
                val lambdaClassName = asmTypeForAnonymousClass(bindingContext, inlineArgument)
                    .internalName.substringAfterLast("/")

                dropInlineSuffix(variableName) == "-$functionName-$lambdaClassName"
            } else {
                // For Kotlin up to 1.3.10
                lambdaOrdinalByLocalVariable(variableName) == lambdaOrdinal
                        && functionNameByLocalVariable(variableName) == functionName
            }
        }
}

fun <T : Any> DebugProcessImpl.invokeInManagerThread(f: (DebuggerContextImpl) -> T?): T? {
    var result: T? = null
    val command: DebuggerCommandImpl = object : DebuggerCommandImpl() {
        override fun action() {
            result = runReadAction { f(debuggerContext) }
        }
    }

    when {
        DebuggerManagerThreadImpl.isManagerThread() ->
            managerThread.invoke(command)
        else ->
            managerThread.invokeAndWait(command)
    }

    return result
}

fun <T : Any> SuspendContextImpl.invokeInSuspendManagerThread(debugProcessImpl: DebugProcessImpl, f: (SuspendContextImpl) -> T?): T? {
    var result: T? = null
    val command: SuspendContextCommandImpl = object : SuspendContextCommandImpl(this) {
        override fun contextAction() {
            result = runReadAction { f(this@invokeInSuspendManagerThread) }
        }
    }

    when {
        DebuggerManagerThreadImpl.isManagerThread() ->
            debugProcessImpl.managerThread.invoke(command)
        else ->
            debugProcessImpl.managerThread.invokeAndWait(command)
    }

    return result
}

private fun lambdaOrdinalByArgument(elementAt: KtFunction, context: BindingContext): Int {
    val type = asmTypeForAnonymousClass(context, elementAt)
    return type.className.substringAfterLast("$").toInt()
}

private fun functionNameByArgument(elementAt: KtFunction, context: BindingContext): String {
    val inlineArgumentDescriptor = InlineUtil.getInlineArgumentDescriptor(elementAt, context)
    return inlineArgumentDescriptor?.containingDeclaration?.name?.asString() ?: "unknown"
}

private fun Location.visibleVariables(debugProcess: DebugProcessImpl): List<LocalVariable> {
    val stackFrame = MockStackFrame(this, debugProcess.virtualMachineProxy.virtualMachine)
    return stackFrame.visibleVariables()
}

// For Kotlin up to 1.3.10
private fun lambdaOrdinalByLocalVariable(name: String): Int = try {
    val nameWithoutPrefix = name.removePrefix(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
    Integer.parseInt(nameWithoutPrefix.substringBefore("$", nameWithoutPrefix))
} catch (e: NumberFormatException) {
    0
}

// For Kotlin up to 1.3.10
private fun functionNameByLocalVariable(name: String): String {
    val nameWithoutPrefix = name.removePrefix(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
    return nameWithoutPrefix.substringAfterLast("$", "unknown")
}

private class MockStackFrame(private val location: Location, private val vm: VirtualMachine) : StackFrame {
    private var visibleVariables: Map<String, LocalVariable>? = null

    override fun location() = location
    override fun thread() = null
    override fun thisObject() = null

    private fun createVisibleVariables() {
        if (visibleVariables == null) {
            val allVariables = location.method().safeVariables() ?: emptyList()
            val map = HashMap<String, LocalVariable>(allVariables.size)

            for (variable in allVariables) {
                if (variable.isVisible(this)) {
                    map[variable.name()] = variable
                }
            }
            visibleVariables = map
        }
    }

    override fun visibleVariables(): List<LocalVariable> {
        createVisibleVariables()
        val mapAsList = ArrayList(visibleVariables!!.values)
        mapAsList.sort()
        return mapAsList
    }

    override fun visibleVariableByName(name: String): LocalVariable? {
        createVisibleVariables()
        return visibleVariables!![name]
    }

    override fun getValue(variable: LocalVariable) = null
    override fun getValues(variables: List<LocalVariable>): Map<LocalVariable, Value> = emptyMap()
    override fun setValue(variable: LocalVariable, value: Value) {
    }

    override fun getArgumentValues(): List<Value> = emptyList()
    override fun virtualMachine() = vm
}

private const val DO_RESUME_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/lang/Object;"
private const val INVOKE_SUSPEND_SIGNATURE = "(Ljava/lang/Object;)Ljava/lang/Object;"

fun isInSuspendMethod(location: Location): Boolean {
    val method = location.method()
    val signature = method.signature()

    for (continuationAsmType in continuationAsmTypes()) {
        if (signature.contains(continuationAsmType.toString()) ||
            (method.name() == DO_RESUME_METHOD_NAME && signature == DO_RESUME_SIGNATURE) ||
            (method.name() == INVOKE_SUSPEND_METHOD_NAME && signature == INVOKE_SUSPEND_SIGNATURE)
        ) return true
    }
    return false
}

fun suspendFunctionFirstLineLocation(location: Location): Int? {
    if (!isInSuspendMethod(location)) {
        return null
    }

    val lineNumber = location.method().location()?.lineNumber()
    if (lineNumber == -1) {
        return null
    }

    return lineNumber
}

fun isOnSuspendReturnOrReenter(location: Location): Boolean {
    val suspendStartLineNumber = suspendFunctionFirstLineLocation(location) ?: return false
    return suspendStartLineNumber == location.lineNumber()
}

fun isLastLineLocationInMethod(location: Location): Boolean {
    val knownLines = location.method().safeAllLineLocations().map { it.lineNumber() }.filter { it != -1 }
    if (knownLines.isEmpty()) {
        return false
    }

    return knownLines.max() == location.lineNumber()
}

fun isOneLineMethod(location: Location): Boolean {
    val allLineLocations = location.method().safeAllLineLocations()
    val firstLine = allLineLocations.firstOrNull()?.lineNumber()
    val lastLine = allLineLocations.lastOrNull()?.lineNumber()

    return firstLine != null && firstLine == lastLine
}

fun findElementAtLine(file: KtFile, line: Int): PsiElement? {
    val lineStartOffset = file.getLineStartOffset(line) ?: return null
    val lineEndOffset = file.getLineEndOffset(line) ?: return null

    return runReadAction {
        var topMostElement: PsiElement? = null
        var elementAt: PsiElement?
        for (offset in lineStartOffset until lineEndOffset) {
            elementAt = file.findElementAt(offset)
            if (elementAt != null) {
                topMostElement = CodeInsightUtils.getTopmostElementAtOffset(elementAt, offset)
                if (topMostElement is KtElement) {
                    break
                }
            }
        }

        topMostElement
    }
}

fun findCallByEndToken(element: PsiElement): KtCallExpression? {
    if (element is KtElement) return null

    return when (element.node.elementType) {
        KtTokens.RPAR -> (element.parent as? KtValueArgumentList)?.parent as? KtCallExpression
        KtTokens.RBRACE -> when (val braceParent = CodeInsightUtils.getTopParentWithEndOffset(element, KtCallExpression::class.java)) {
            is KtCallExpression -> braceParent
            is KtLambdaArgument -> braceParent.parent as? KtCallExpression
            is KtValueArgument -> (braceParent.parent as? KtValueArgumentList)?.parent as? KtCallExpression
            else -> null
        }
        else -> null
    }
}

val DebuggerContextImpl.canRunEvaluation: Boolean
    get() = debugProcess?.canRunEvaluation ?: false

val DebugProcessImpl.canRunEvaluation: Boolean
    get() = suspendManager.pausedContext != null