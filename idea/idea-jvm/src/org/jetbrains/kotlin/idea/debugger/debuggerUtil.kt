/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.psi.PsiElement
import com.sun.jdi.*
import com.sun.tools.jdi.LocalVariableImpl
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.coroutines.DO_RESUME_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.coroutines.continuationAsmTypes
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.refactoring.getLineEndOffset
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import java.util.*

fun Method.safeAllLineLocations(): List<Location> {
    return DebuggerUtilsEx.allLineLocations(this) ?: emptyList()
}

fun ReferenceType.safeAllLineLocations(): List<Location> {
    return DebuggerUtilsEx.allLineLocations(this) ?: emptyList()
}

fun Method.safeLocationsOfLine(line: Int): List<Location> {
    return try {
        locationsOfLine(line)
    } catch (e: AbsentInformationException) {
        emptyList()
    }
}

fun Method.safeVariables(): List<LocalVariable>? {
    return try {
        variables()
    } catch (e: AbsentInformationException) {
        null
    }
}

fun Method.safeArguments(): List<LocalVariable>? {
    return try {
        arguments()
    } catch (e: AbsentInformationException) {
        null
    }
}

fun Method.safeReturnType(): Type? {
    return try {
        returnType()
    } catch (e: ClassNotLoadedException) {
        null
    }
}

fun Location.safeSourceName(): String? {
    return try {
        sourceName()
    } catch (e: AbsentInformationException) {
        null
    } catch (e: InternalError) {
        null
    }
}

fun Location.safeLineNumber(): Int {
    return DebuggerUtilsEx.getLineNumber(this, false)
}

fun Location.safeSourceLineNumber(): Int {
    return DebuggerUtilsEx.getLineNumber(this, true)
}

fun Location.safeMethod(): Method? {
    return DebuggerUtilsEx.getMethod(this)
}

fun isInsideInlineFunctionBody(visibleVariables: List<LocalVariableProxyImpl>): Boolean {
    return visibleVariables.any { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) }
}

fun numberOfInlinedFunctions(visibleVariables: List<LocalVariableProxyImpl>): Int {
    return visibleVariables.count { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) }
}

fun isInsideInlineArgument(inlineArgument: KtFunction, location: Location, debugProcess: DebugProcessImpl): Boolean {
    val visibleVariables = location.visibleVariables(debugProcess)
    val markerLocalVariables = visibleVariables.filter { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT) }

    val context = KotlinDebuggerCaches.getOrCreateTypeMapper(inlineArgument).bindingContext
    val lambdaOrdinal = runReadAction { lambdaOrdinalByArgument(inlineArgument, context) }
    val functionName = runReadAction { functionNameByArgument(inlineArgument, context) }

    return markerLocalVariables
            .map { it.name() }
            .any { variableName ->
                lambdaOrdinalByLocalVariable(variableName) == lambdaOrdinal && functionNameByLocalVariable(variableName) == functionName
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

private fun lambdaOrdinalByArgument(elementAt: KtFunction, context: BindingContext): Int {
    val type = CodegenBinding.asmTypeForAnonymousClass(context, elementAt)
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

private fun lambdaOrdinalByLocalVariable(name: String): Int {
    try {
        val nameWithoutPrefix = name.removePrefix(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT)
        return Integer.parseInt(nameWithoutPrefix.substringBefore("$", nameWithoutPrefix))
    }
    catch(e: NumberFormatException) {
        return 0
    }
}

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

            for (allVariable in allVariables) {
                val variable = allVariable as LocalVariableImpl
                val name = variable.name()
                if (variable.isVisible(this)) {
                    map.put(name, variable)
                }
            }
            visibleVariables = map
        }
    }

    override fun visibleVariables(): List<LocalVariable> {
        createVisibleVariables()
        val mapAsList = ArrayList(visibleVariables!!.values)
        Collections.sort(mapAsList)
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

    return topMostElement
}

fun findCallByEndToken(element: PsiElement): KtCallExpression? {
    if (element is KtElement) return null

    return when (element.node.elementType) {
        KtTokens.RPAR -> (element.parent as? KtValueArgumentList)?.parent as? KtCallExpression
        KtTokens.RBRACE -> {
            val braceParent = CodeInsightUtils.getTopParentWithEndOffset(element, KtCallExpression::class.java)
            when (braceParent) {
                is KtCallExpression -> braceParent
                is KtLambdaArgument -> braceParent.parent as? KtCallExpression
                is KtValueArgument -> (braceParent.parent as? KtValueArgumentList)?.parent as? KtCallExpression
                else -> null
            }
        }
        else -> null
    }
}
