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

import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.sun.jdi.Value
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.CONTINUATION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.inline.INLINE_FUN_VAR_SUFFIX
import org.jetbrains.kotlin.codegen.inline.isFakeLocalVariableForInline
import org.jetbrains.kotlin.idea.debugger.evaluate.THIS_NAME
import org.jetbrains.kotlin.idea.debugger.evaluate.VariableFinder
import org.jetbrains.kotlin.utils.getSafe
import java.lang.reflect.Modifier
import java.util.*
import org.jetbrains.kotlin.idea.debugger.evaluate.LOG as DebuggerLog

class KotlinStackFrame(frame: StackFrameProxyImpl) : JavaStackFrame(StackFrameDescriptorImpl(frame, MethodsTracker()), true) {
    private val kotlinVariableViewService = ToggleKotlinVariablesState.getService()

    override fun superBuildVariables(evaluationContext: EvaluationContextImpl, children: XValueChildrenList) {
        if (!kotlinVariableViewService.kotlinVariableView) {
            return super.superBuildVariables(evaluationContext, children)
        }

        val nodeManager = evaluationContext.debugProcess.xdebugProcess!!.nodeManager

        removeThisObjectIfNeeded(evaluationContext, children)

        fun addItem(variable: LocalVariableProxyImpl) {
            val variableDescriptor = nodeManager.getLocalVariableDescriptor(null, variable)
            children.add(JavaValue.create(null, variableDescriptor, evaluationContext, nodeManager, false))
        }

        val (thisReferences, otherVariables) = visibleVariables
            .partition { it.name() == THIS_NAME || it.name().startsWith("$THIS_NAME ") }

        thisReferences.forEach(::addItem)
        otherVariables.forEach(::addItem)
    }

    private fun removeThisObjectIfNeeded(evaluationContext: EvaluationContextImpl, children: XValueChildrenList) {
        val thisObject = evaluationContext.frameProxy?.thisObject() ?: return
        if (!thisObject.type().isSubtype(VariableFinder.CONTINUATION_TYPE)) {
            return
        }

        ExistingVariable.find(children, "this")?.remove()
    }

    // Very Dirty Work-around.
    // Hopefully, there will be an API for that in 2019.1.
    private class ExistingVariable(
        private val children: XValueChildrenList,
        private val index: Int,
        private val name: String,
        private val value: XValue,
        private val size: Int
    ) {
        companion object {
            fun find(children: XValueChildrenList, name: String): ExistingVariable? {
                val size = children.size()
                for (i in 0 until size) {
                    if (children.getName(i) == name) {
                        val valueDescriptor = (children.getValue(i) as? JavaValue)?.descriptor
                        @Suppress("FoldInitializerAndIfToElvis")
                        if (valueDescriptor !is ThisDescriptorImpl) {
                            return null
                        }

                        return ExistingInstanceThis(children, i, name, children.getValue(i), size)
                    }
                }

                return null
            }
        }

        fun remove() {
            val (names, values) = getLists() ?: return
            names.removeAt(index)
            values.removeAt(index)
        }

        private fun getLists(): Lists? {
            if (children.size() != size) {
                throw IllegalStateException("Children list was modified")
            }

            var namesList: MutableList<Any?>? = null
            var valuesList: MutableList<Any?>? = null

            for (field in XValueChildrenList::class.java.declaredFields) {
                val mods = field.modifiers
                if (Modifier.isPrivate(mods) && Modifier.isFinal(mods) && !Modifier.isStatic(mods) && field.type == List::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    val list = (field.getSafe(children) as? MutableList<Any?>)?.takeIf { it.size == size } ?: continue

                    if (list[index] == THIS_NAME) {
                        namesList = list
                    } else if (list[index] === value) {
                        valuesList = list
                    }
                }

                if (namesList != null && valuesList != null) {
                    return Lists(namesList, valuesList)
                }
            }

            DebuggerLog.error(
                "Can't find name/value lists, existing fields: "
                        + Arrays.toString(XValueChildrenList::class.java.declaredFields)
            )

            return null
        }

        private data class Lists(val names: MutableList<Any?>, val values: MutableList<Any?>)
    }

    override fun getVisibleVariables(): List<LocalVariableProxyImpl> {
        val allVisibleVariables = super.getStackFrameProxy().safeVisibleVariables()

        if (!kotlinVariableViewService.kotlinVariableView) {
            return allVisibleVariables.map { variable ->
                if (isFakeLocalVariableForInline(variable.name())) variable.wrapSyntheticInlineVariable() else variable
            }
        }

        return allVisibleVariables.asSequence()
            .filter { !isHidden(it) }
            .map { remapVariableName(it) }
            .distinctBy { it.name() }
            .toList()
            .sortedBy { it.variable }
    }

    private fun isHidden(variable: LocalVariableProxyImpl): Boolean {
        val name = variable.name()
        return isFakeLocalVariableForInline(name)
                || name.endsWith(INLINE_FUN_VAR_SUFFIX)
                || name == CONTINUATION_PARAMETER_NAME.asString()
    }

    private fun remapVariableName(variable: LocalVariableProxyImpl) = with(variable) {
        when {
            isLabeledThisReference() -> {
                val label = variable.name().drop(AsmUtil.LABELED_THIS_PARAMETER.length)
                clone(withName = "$THIS_NAME (@$label)")
            }
            else -> variable
        }
    }

    private fun LocalVariableProxyImpl.isLabeledThisReference(): Boolean {
        @Suppress("ConvertToStringTemplate")
        return name().startsWith(AsmUtil.LABELED_THIS_PARAMETER)
    }
}

private fun LocalVariableProxyImpl.clone(withName: String): LocalVariableProxyImpl {
    return object : LocalVariableProxyImpl(frame, variable) {
        override fun name() = withName
    }
}

private fun LocalVariableProxyImpl.wrapSyntheticInlineVariable(): LocalVariableProxyImpl {
    val proxyWrapper = object : StackFrameProxyImpl(frame.threadProxy(), frame.stackFrame, frame.indexFromBottom) {
        override fun getValue(localVariable: LocalVariableProxyImpl): Value {
            return frame.virtualMachine.mirrorOfVoid()
        }
    }
    return LocalVariableProxyImpl(proxyWrapper, variable)
}