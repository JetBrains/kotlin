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
import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.descriptors.data.DescriptorData
import com.intellij.debugger.impl.descriptors.data.DisplayKey
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.debugger.ui.impl.watch.ThisDescriptorImpl
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiExpression
import com.intellij.util.IncorrectOperationException
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.sun.jdi.ObjectReference
import com.sun.jdi.ReferenceType
import com.sun.jdi.Type
import com.sun.jdi.Value
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.AsmUtil.THIS
import org.jetbrains.kotlin.codegen.DESTRUCTURED_LAMBDA_ARGUMENT_VARIABLE_PREFIX
import org.jetbrains.kotlin.codegen.coroutines.CONTINUATION_VARIABLE_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_CONTINUATION_PARAMETER
import org.jetbrains.kotlin.codegen.inline.INLINE_FUN_VAR_SUFFIX
import org.jetbrains.kotlin.codegen.inline.isFakeLocalVariableForInline
import org.jetbrains.org.objectweb.asm.Type as AsmType
import org.jetbrains.kotlin.utils.getSafe
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerEvaluator
import java.lang.reflect.Modifier
import java.util.*

class KotlinStackFrame(frame: StackFrameProxyImpl) : JavaStackFrame(StackFrameDescriptorImpl(frame, MethodsTracker()), true) {
    private companion object {
        private val LOG = Logger.getInstance(this::class.java)
    }

    private val kotlinVariableViewService = ToggleKotlinVariablesState.getService()

    private val kotlinEvaluator by lazy {
        val debugProcess = descriptor.debugProcess as DebugProcessImpl // Cast as in JavaStackFrame
        KotlinDebuggerEvaluator(debugProcess, this@KotlinStackFrame)
    }

    override fun getEvaluator() = kotlinEvaluator

    override fun superBuildVariables(evaluationContext: EvaluationContextImpl, children: XValueChildrenList) {
        if (!kotlinVariableViewService.kotlinVariableView) {
            return super.superBuildVariables(evaluationContext, children)
        }

        val nodeManager = evaluationContext.debugProcess.xdebugProcess?.nodeManager

        fun addItem(variable: LocalVariableProxyImpl) {
            if (nodeManager == null) {
                return
            }

            val variableDescriptor = nodeManager.getLocalVariableDescriptor(null, variable)
            children.add(JavaValue.create(null, variableDescriptor, evaluationContext, nodeManager, false))
        }

        val (thisReferences, otherVariables) = visibleVariables
            .partition { it.name() == THIS || it is ThisLocalVariable }

        if (!removeSyntheticThisObject(evaluationContext, children, thisReferences) && thisReferences.isNotEmpty()) {
            val thisLabels = thisReferences.asSequence()
                .filterIsInstance<ThisLocalVariable>()
                .mapNotNullTo(hashSetOf()) { it.label }

            remapThisObjectForOuterThis(evaluationContext, children, thisLabels)
        }

        thisReferences.forEach(::addItem)
        otherVariables.forEach(::addItem)
    }

    private fun removeSyntheticThisObject(
        evaluationContext: EvaluationContextImpl,
        children: XValueChildrenList,
        thisReferences: List<LocalVariableProxyImpl>
    ): Boolean {
        val thisObject = evaluationContext.frameProxy?.thisObject() ?: return false

        if (thisObject.type().isSubtype(CONTINUATION_TYPE)) {
            ExistingInstanceThis.find(children)?.remove()
            return true
        }

        val thisObjectType = thisObject.type()
        if (thisObjectType.isSubtype(Function::class.java.name) && '$' in thisObjectType.signature()) {
            val existingThis = ExistingInstanceThis.find(children)
            if (existingThis != null) {
                existingThis.remove()
                val javaValue = existingThis.value as? JavaValue
                if (javaValue != null) {
                    attachCapturedThisFromLambda(evaluationContext, children, javaValue, thisReferences)
                }
            }
            return true
        }

        return removeCallSiteThisInInlineFunction(evaluationContext, children)
    }

    private fun removeCallSiteThisInInlineFunction(evaluationContext: EvaluationContextImpl, children: XValueChildrenList): Boolean {
        val frameProxy = evaluationContext.frameProxy

        val variables = frameProxy?.safeVisibleVariables() ?: return false
        val inlineDepth = getInlineDepth(variables)
        val declarationSiteThis = variables.firstOrNull { v ->
            val name = v.name()
            name.endsWith(INLINE_FUN_VAR_SUFFIX) && name.dropInlineSuffix() == AsmUtil.INLINE_DECLARATION_SITE_THIS
        }

        if (inlineDepth > 0 && declarationSiteThis != null) {
            ExistingInstanceThis.find(children)?.remove()
            return true
        }

        return false
    }

    private fun attachCapturedThisFromLambda(
        evaluationContext: EvaluationContextImpl,
        children: XValueChildrenList,
        javaValue: JavaValue,
        thisReferences: List<LocalVariableProxyImpl>
    ) {
        try {
            val value = javaValue.descriptor.calcValue(evaluationContext) as? ObjectReference ?: return
            val thisField = value.referenceType().fieldByName(AsmUtil.CAPTURED_THIS_FIELD) ?: return
            val thisValue = value.getValue(thisField) as? ObjectReference ?: return
            val thisType = thisValue.referenceType()
            val unsafeLabel = generateThisLabelUnsafe(thisType) ?: return
            val label = checkLabel(unsafeLabel)

            if (label != null) {
                val thisName = getThisName(label)

                if (thisReferences.any { it.name() == thisName }) {
                    // Avoid label duplication
                    return
                }
            }

            val thisName = when {
                thisReferences.isEmpty() -> THIS
                label != null -> getThisName(label)
                else -> "$THIS (anonymous fun)"
            }

            val nodeManager = evaluationContext.debugProcess.xdebugProcess?.nodeManager ?: return
            val thisDescriptor = nodeManager.getDescriptor(this.descriptor, LabeledThisData(thisName, thisValue))
            children.add(JavaValue.create(null, thisDescriptor, evaluationContext, nodeManager, false))
        } catch (e: EvaluateException) {
            // do nothing
        }
    }

    private fun remapThisObjectForOuterThis(
        evaluationContext: EvaluationContextImpl,
        children: XValueChildrenList,
        existingThisLabels: Set<String>
    ) {
        val thisObject = evaluationContext.frameProxy?.thisObject() ?: return
        val variable = ExistingInstanceThis.find(children) ?: return

        val thisLabel = generateThisLabel(thisObject.referenceType())?.takeIf { it !in existingThisLabels }
        if (thisLabel == null) {
            variable.remove()
            return
        }

        // add additional checks?
        variable.remapName(getThisName(thisLabel))
    }

    // Very Dirty Work-around.
    // Hopefully, there will be an API for that in 2019.1.
    private class ExistingInstanceThis(
        private val children: XValueChildrenList,
        private val index: Int,
        val value: XValue,
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

            LOG.error(
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

        val inlineDepth = getInlineDepth(allVisibleVariables)

        val (thisVariables, otherVariables) = allVisibleVariables.asSequence()
            .filter { !isHidden(it, inlineDepth) }
            .partition {
                it.name() == THIS
                        || it.name() == AsmUtil.THIS_IN_DEFAULT_IMPLS
                        || it.name().startsWith(AsmUtil.LABELED_THIS_PARAMETER)
                        || (INLINED_THIS_REGEX.matches(it.name()))
            }

        val (mainThis, otherThis) = thisVariables
            .sortedByDescending { it.variable }
            .let { it.firstOrNull() to it.drop(1) }

        val remappedMainThis = mainThis?.clone(THIS, null)
        val remappedOther = (otherThis + otherVariables).map { it.remapVariableNameIfNeeded() }
        return (listOfNotNull(remappedMainThis) + remappedOther).sortedBy { it.variable }
    }

    private fun isHidden(variable: LocalVariableProxyImpl, inlineDepth: Int): Boolean {
        val name = variable.name()
        return isFakeLocalVariableForInline(name)
                || name.startsWith(DESTRUCTURED_LAMBDA_ARGUMENT_VARIABLE_PREFIX)
                || name.startsWith(AsmUtil.LOCAL_FUNCTION_VARIABLE_PREFIX)
                || getInlineDepth(variable.name()) != inlineDepth
                || name == CONTINUATION_VARIABLE_NAME
                || name == SUSPEND_FUNCTION_CONTINUATION_PARAMETER
    }

    private fun LocalVariableProxyImpl.remapVariableNameIfNeeded(): LocalVariableProxyImpl {
        val name = this.name().dropInlineSuffix()

        @Suppress("ConvertToStringTemplate")
        return when {
            isLabeledThisReference() -> {
                val label = name.drop(AsmUtil.LABELED_THIS_PARAMETER.length)
                clone(getThisName(label), label)
            }
            name == AsmUtil.THIS_IN_DEFAULT_IMPLS -> clone(THIS + " (outer)", null)
            name == AsmUtil.RECEIVER_PARAMETER_NAME -> clone(THIS + " (receiver)", null)
            INLINED_THIS_REGEX.matches(name) -> {
                val label = generateThisLabel(frame.getValue(this)?.type())
                if (label != null) {
                    clone(getThisName(label), label)
                } else {
                    this@remapVariableNameIfNeeded
                }
            }
            name != this.name() -> {
                object : LocalVariableProxyImpl(frame, variable) {
                    override fun name() = name
                }
            }
            else -> this@remapVariableNameIfNeeded
        }
    }

    private fun generateThisLabel(type: Type?): String? {
        return checkLabel(generateThisLabelUnsafe(type) ?: return null)
    }

    private fun generateThisLabelUnsafe(type: Type?): String? {
        val referenceType = type as? ReferenceType ?: return null
        return referenceType.name().substringAfterLast('.').substringAfterLast('$')
    }

    private fun checkLabel(label: String): String? {
        if (label.isEmpty() || label.all { it.isDigit() }) {
            return null
        }

        return label
    }

    private fun String.dropInlineSuffix(): String {
        val depth = getInlineDepth(this)
        if (depth == 0) {
            return this
        }

        return dropLast(depth * INLINE_FUN_VAR_SUFFIX.length)
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

private fun getThisName(label: String): String {
    return "$THIS (@$label)"
}

private class LabeledThisData(val name: String, val value: ObjectReference) : DescriptorData<ValueDescriptorImpl>() {
    override fun createDescriptorImpl(project: Project): ValueDescriptorImpl {
        return object : ValueDescriptorImpl(project, value) {
            override fun getName() = this@LabeledThisData.name
            override fun calcValue(evaluationContext: EvaluationContextImpl?) = value
            override fun canSetValue() = false

            override fun getDescriptorEvaluation(context: DebuggerContext?): PsiExpression {
                // TODO change to labeled this
                val elementFactory = JavaPsiFacade.getElementFactory(myProject)
                try {
                    return elementFactory.createExpressionFromText("this", null)
                } catch (e: IncorrectOperationException) {
                    throw EvaluateException(e.message, e)
                }
            }
        }
    }

    override fun getDisplayKey(): DisplayKey<ValueDescriptorImpl> = SimpleDisplayKey(this)
    override fun equals(other: Any?) = other is LabeledThisData && other.name == name
    override fun hashCode() = name.hashCode()
}