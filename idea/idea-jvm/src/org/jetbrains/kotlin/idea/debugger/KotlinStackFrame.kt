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
import com.intellij.debugger.ui.impl.watch.ThisDescriptorImpl
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

        fun addItem(variable: LocalVariableProxyImpl) {
            val variableDescriptor = nodeManager.getLocalVariableDescriptor(null, variable)
            children.add(JavaValue.create(null, variableDescriptor, evaluationContext, nodeManager, false))
        }

        val (thisReferences, otherVariables) = visibleVariables
            .partition { it.name() == AsmUtil.THIS || it is ThisLocalVariable }

        if (!removeThisObjectForContinuation(evaluationContext, children) && thisReferences.isNotEmpty()) {
            val thisLabels = thisReferences.asSequence()
                .filterIsInstance<ThisLocalVariable>()
                .mapNotNullTo(hashSetOf()) { it.label }

            remapThisObjectForOuterThis(evaluationContext, children, thisLabels)
        }

        thisReferences.forEach(::addItem)
        otherVariables.forEach(::addItem)
    }

    private fun removeThisObjectForContinuation(evaluationContext: EvaluationContextImpl, children: XValueChildrenList): Boolean {
        val thisObject = evaluationContext.frameProxy?.thisObject() ?: return false
        if (!thisObject.type().isSubtype(VariableFinder.CONTINUATION_TYPE)) {
            return false
        }

        ExistingInstanceThis.find(children)?.remove()
        return true
    }

    private fun remapThisObjectForOuterThis(
        evaluationContext: EvaluationContextImpl,
        children: XValueChildrenList,
        existingThisLabels: Set<String>
    ) {
        val thisObject = evaluationContext.frameProxy?.thisObject() ?: return
        val variable = ExistingInstanceThis.find(children) ?: return

        val thisLabel = thisObject.referenceType().name().substringAfterLast('.').substringAfterLast('$')
        if (thisLabel.isEmpty() || thisLabel in existingThisLabels || thisLabel.all { it.isDigit() }) {
            variable.remove()
            return
        }

        variable.remapName(getThisName(thisLabel))
    }

    // Very Dirty Work-around.
    // Hopefully, there will be an API for that in 2019.1.
    private class ExistingInstanceThis(
        private val children: XValueChildrenList,
        private val index: Int,
        private val value: XValue,
        private val size: Int
    ) {
        companion object {
            private const val THIS_NAME = "this"

            fun find(children: XValueChildrenList): ExistingInstanceThis? {
                val size = children.size()
                for (i in 0 until size) {
                    if (children.getName(i) == THIS_NAME) {
                        val valueDescriptor = (children.getValue(i) as? JavaValue)?.descriptor
                        @Suppress("FoldInitializerAndIfToElvis")
                        if (valueDescriptor !is ThisDescriptorImpl) {
                            return null
                        }

                        return ExistingInstanceThis(children, i, children.getValue(i), size)
                    }
                }

                return null
            }
        }

        fun remapName(newName: String) {
            val (names, _) = getLists() ?: return
            names[index] = newName
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

        val (thisVariables, otherVariables) = allVisibleVariables.asSequence()
            .filter { !isHidden(it) }
            .partition { it.name() == AsmUtil.THIS || it.name().startsWith(AsmUtil.LABELED_THIS_PARAMETER) }

        val (mainThis, otherThis) = thisVariables
            .sortedByDescending { it.variable }
            .let { it.firstOrNull() to it.drop(1) }

        val remappedMainThis = mainThis?.clone(AsmUtil.THIS, null)
        val remappedOtherThis = otherThis.map { it.remapThisName() }

        return (listOfNotNull(remappedMainThis) + remappedOtherThis + otherVariables)
            .sortedBy { it.variable }
    }

    private fun isHidden(variable: LocalVariableProxyImpl): Boolean {
        val name = variable.name()
        return isFakeLocalVariableForInline(name)
                || name.endsWith(INLINE_FUN_VAR_SUFFIX)
                || name == CONTINUATION_PARAMETER_NAME.asString()
    }

    private fun LocalVariableProxyImpl.remapThisName(): LocalVariableProxyImpl {
        return when {
            isLabeledThisReference() -> {
                val label = this@remapThisName.name().drop(AsmUtil.LABELED_THIS_PARAMETER.length)
                clone(getThisName(label), label)
            }
            this@remapThisName.name() == AsmUtil.RECEIVER_PARAMETER_NAME -> clone(AsmUtil.THIS + " (receiver)", null)
            else -> this@remapThisName
        }
    }

    private fun getThisName(label: String): String {
        return "$THIS_NAME (@$label)"
    }

    private fun LocalVariableProxyImpl.clone(name: String, label: String?): LocalVariableProxyImpl {
        return object : LocalVariableProxyImpl(frame, variable), ThisLocalVariable {
            override fun name() = name
            override val label = label
        }
    }

    private fun LocalVariableProxyImpl.isLabeledThisReference(): Boolean {
        @Suppress("ConvertToStringTemplate")
        return name().startsWith(AsmUtil.LABELED_THIS_PARAMETER)
    }
}

private interface ThisLocalVariable {
    val label: String?
}

private fun LocalVariableProxyImpl.wrapSyntheticInlineVariable(): LocalVariableProxyImpl {
    val proxyWrapper = object : StackFrameProxyImpl(frame.threadProxy(), frame.stackFrame, frame.indexFromBottom) {
        override fun getValue(localVariable: LocalVariableProxyImpl): Value {
            return frame.virtualMachine.mirrorOfVoid()
        }
    }
    return LocalVariableProxyImpl(proxyWrapper, variable)
}