/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stackFrame

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.xdebugger.frame.XValueChildrenList
import com.sun.jdi.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.AsmUtil.THIS
import org.jetbrains.kotlin.codegen.DESTRUCTURED_LAMBDA_ARGUMENT_VARIABLE_PREFIX
import org.jetbrains.kotlin.codegen.coroutines.CONTINUATION_VARIABLE_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.inline.INLINE_FUN_VAR_SUFFIX
import org.jetbrains.kotlin.codegen.inline.isFakeLocalVariableForInline
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerEvaluator

open class KotlinStackFrame(stackFrameDescriptorImpl: StackFrameDescriptorImpl) : JavaStackFrame(stackFrameDescriptorImpl, true) {
    private val kotlinVariableViewService = ToggleKotlinVariablesState.getService()

    constructor(frame: StackFrameProxyImpl) : this(StackFrameDescriptorImpl(frame, MethodsTracker()))

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

        val (thisVariables, otherVariables) = visibleVariables
            .partition { it.name() == THIS || it is ThisLocalVariable }

        val existingVariables = ExistingVariables(thisVariables, otherVariables)

        if (!removeSyntheticThisObject(evaluationContext, children, existingVariables) && thisVariables.isNotEmpty()) {
            remapThisObjectForOuterThis(evaluationContext, children, existingVariables)
        }

        thisVariables.forEach(::addItem)
        otherVariables.forEach(::addItem)
    }

    private fun removeSyntheticThisObject(
        evaluationContext: EvaluationContextImpl,
        children: XValueChildrenList,
        existingVariables: ExistingVariables
    ): Boolean {
        val thisObject = evaluationContext.frameProxy?.thisObject() ?: return false

        if (thisObject.type().isSubtype(CONTINUATION_TYPE)) {
            ExistingInstanceThisRemapper.find(children)?.remove()
            return true
        }

        val thisObjectType = thisObject.type()
        if (thisObjectType.isSubtype(Function::class.java.name) && '$' in thisObjectType.signature()) {
            val existingThis = ExistingInstanceThisRemapper.find(children)
            if (existingThis != null) {
                existingThis.remove()
                val containerJavaValue = existingThis.value as? JavaValue
                val containerValue = containerJavaValue?.descriptor?.calcValue(evaluationContext) as? ObjectReference
                if (containerValue != null) {
                    attachCapturedValues(evaluationContext, children, containerValue, existingVariables)
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
            name.endsWith(INLINE_FUN_VAR_SUFFIX) && dropInlineSuffix(name) == AsmUtil.INLINE_DECLARATION_SITE_THIS
        }

        if (inlineDepth > 0 && declarationSiteThis != null) {
            ExistingInstanceThisRemapper.find(children)?.remove()
            return true
        }

        return false
    }

    private fun attachCapturedValues(
        evaluationContext: EvaluationContextImpl,
        children: XValueChildrenList,
        containerValue: ObjectReference,
        existingVariables: ExistingVariables
    ) {
        val nodeManager = evaluationContext.debugProcess.xdebugProcess?.nodeManager ?: return

        attachCapturedValues(containerValue, existingVariables) { valueData ->
            val valueDescriptor = nodeManager.getDescriptor(this.descriptor, valueData)
            children.add(JavaValue.create(null, valueDescriptor, evaluationContext, nodeManager, false))
        }
    }

    private fun remapThisObjectForOuterThis(
        evaluationContext: EvaluationContextImpl,
        children: XValueChildrenList,
        existingVariables: ExistingVariables
    ) {
        val thisObject = evaluationContext.frameProxy?.thisObject() ?: return
        val variable = ExistingInstanceThisRemapper.find(children) ?: return

        val thisLabel = generateThisLabel(thisObject.referenceType())
        val thisName = thisLabel?.let(::getThisName)

        if (thisName == null || !existingVariables.add(ExistingVariable.LabeledThis(thisLabel))) {
            variable.remove()
            return
        }

        // add additional checks?
        variable.remapName(getThisName(thisLabel))
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

        val remappedMainThis = mainThis?.remapThisVariableIfNeeded(THIS)
        val remappedOther = (otherThis + otherVariables).map { it.remapThisVariableIfNeeded() }
        return (listOfNotNull(remappedMainThis) + remappedOther).sortedBy { it.variable }
    }

    private fun isHidden(variable: LocalVariableProxyImpl, inlineDepth: Int): Boolean {
        val name = variable.name()
        return isFakeLocalVariableForInline(name)
                || name.startsWith(DESTRUCTURED_LAMBDA_ARGUMENT_VARIABLE_PREFIX)
                || name.startsWith(AsmUtil.LOCAL_FUNCTION_VARIABLE_PREFIX)
                || getInlineDepth(variable.name()) != inlineDepth
                || name == CONTINUATION_VARIABLE_NAME
                || name == SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
    }

    private fun LocalVariableProxyImpl.remapThisVariableIfNeeded(customName: String? = null): LocalVariableProxyImpl {
        val name = dropInlineSuffix(this.name())

        @Suppress("ConvertToStringTemplate")
        return when {
            isLabeledThisReference() -> {
                val label = name.drop(AsmUtil.LABELED_THIS_PARAMETER.length)
                clone(customName ?: getThisName(label), label)
            }
            name == AsmUtil.THIS_IN_DEFAULT_IMPLS -> clone(customName ?: THIS + " (outer)", null)
            name == AsmUtil.RECEIVER_PARAMETER_NAME -> clone(customName ?: THIS + " (receiver)", null)
            INLINED_THIS_REGEX.matches(name) -> {
                val label = generateThisLabel(frame.getValue(this)?.type())
                if (label != null) {
                    clone(customName ?: getThisName(label), label)
                } else {
                    this@remapThisVariableIfNeeded
                }
            }
            name != this.name() -> {
                object : LocalVariableProxyImpl(frame, variable) {
                    override fun name() = name
                }
            }
            else -> this@remapThisVariableIfNeeded
        }
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

    override fun equals(other: Any?) : Boolean {
        val eq = super.equals(other)
        return other is KotlinStackFrame && eq
    }
}

interface ThisLocalVariable {
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