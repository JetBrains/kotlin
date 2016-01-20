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
import com.sun.jdi.*
import com.sun.tools.jdi.LocalVariableImpl
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtFunction
import java.util.*

fun isInsideInlineFunctionBody(visibleVariables: List<LocalVariable>): Boolean {
    return visibleVariables.any { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) }
}

fun numberOfInlinedFunctions(visibleVariables: List<LocalVariable>): Int {
    return visibleVariables.count { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) }
}

fun isInsideInlineArgument(inlineArgument: KtFunction, location: Location, debugProcess: DebugProcessImpl): Boolean {
    val visibleVariables = location.visibleVariables(debugProcess)
    val lambdaOrdinalIndex = runReadAction { lambdaOrdinalIndex(inlineArgument) }
    val markerLocalVariables = visibleVariables.filter { it.name().startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT) }
    return markerLocalVariables.firstOrNull { lambdaOrdinal(it.name()) == lambdaOrdinalIndex } != null
}

private fun lambdaOrdinalIndex(elementAt: KtFunction): Int {
    val typeMapper = KotlinPositionManagerCache.getOrCreateTypeMapper(elementAt)

    val type = CodegenBinding.asmTypeForAnonymousClass(typeMapper.bindingContext, elementAt)
    return type.className.substringAfterLast("$").toInt()
}

private fun Location.visibleVariables(debugProcess: DebugProcessImpl): List<LocalVariable> {
    val stackFrame = MockStackFrame(this, debugProcess.virtualMachineProxy.virtualMachine)
    return stackFrame.visibleVariables()
}

private fun lambdaOrdinal(name: String): Int {
    return try {
        return name.substringAfter(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT).toInt()
    }
    catch(e: NumberFormatException) {
        0
    }
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
