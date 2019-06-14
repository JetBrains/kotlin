/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stackFrame

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.descriptors.data.DescriptorData
import com.intellij.debugger.impl.descriptors.data.DisplayKey
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiExpression
import com.intellij.xdebugger.frame.XValueModifier
import com.sun.jdi.*

data class CapturedValueData(
    val valueName: String,
    val obj: ObjectReference,
    val field: Field
) : DescriptorData<ValueDescriptorImpl>() {
    override fun createDescriptorImpl(project: Project): ValueDescriptorImpl {
        val fieldDescriptor = FieldDescriptorImpl(project, obj, field)
        return CustomFieldDescriptor(valueName, fieldDescriptor)
    }

    private class CustomFieldDescriptor(
        val valueName: String,
        val delegate: FieldDescriptorImpl
    ) : ValueDescriptorImpl(delegate.project) {
        override fun getName() = valueName

        override fun calcValue(evaluationContext: EvaluationContextImpl?): Value = delegate.calcValue(evaluationContext)
        override fun getDescriptorEvaluation(context: DebuggerContext?): PsiExpression = delegate.getDescriptorEvaluation(context)
        override fun getModifier(value: JavaValue?): XValueModifier = delegate.getModifier(value)
    }

    override fun getDisplayKey(): DisplayKey<ValueDescriptorImpl> = SimpleDisplayKey(field)
}