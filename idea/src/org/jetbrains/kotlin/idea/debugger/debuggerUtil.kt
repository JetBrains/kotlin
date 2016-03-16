/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.sun.jdi.*
import com.sun.tools.jdi.LocalVariableImpl
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import java.util.*

fun isInsideInlineFunctionBody(visibleVariables: List<LocalVariableProxyImpl>): Boolean {
    return visibleVariables.any { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) }
}

fun numberOfInlinedFunctions(visibleVariables: List<LocalVariableProxyImpl>): Int {
    return visibleVariables.count { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) }
}

fun isInsideInlineArgument(inlineArgument: KtFunction, location: Location, debugProcess: DebugProcessImpl): Boolean {
    val visibleVariables = location.visibleVariables(debugProcess)

    val context = KotlinDebuggerCaches.getOrCreateTypeMapper(inlineArgument).bindingContext

    val lambdaOrdinal = runReadAction { lambdaOrdinalByArgument(inlineArgument, context) }
    val markerLocalVariables = visibleVariables.filter { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT) }

    val functionName = runReadAction { functionNameByArgument(inlineArgument, context) }

    return markerLocalVariables.firstOrNull {
        lambdaOrdinalByLocalVariable(it.name()) == lambdaOrdinal && functionNameByLocalVariable(it.name()) == functionName
    } != null
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
            val allVariables = location.method().variables()
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
