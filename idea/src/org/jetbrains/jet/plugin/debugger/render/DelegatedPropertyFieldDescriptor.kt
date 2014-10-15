package org.jetbrains.jet.plugin.debugger.render

import com.intellij.openapi.project.Project
import com.sun.jdi.Field
import org.jetbrains.jet.lang.resolve.java.JvmAbi
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.sun.jdi.Value
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.DebuggerContext
import com.intellij.psi.PsiExpression
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.tree.FieldDescriptor
import com.sun.jdi.ObjectReference

class DelegatedPropertyFieldDescriptor(
        val project: Project,
        val computedValueFromGetter: Value,
        val objectRef: ObjectReference,
        val delegate: Field
): ValueDescriptorImpl(project, computedValueFromGetter), FieldDescriptor {
    override fun getField() = delegate
    override fun getObject() = objectRef

    override fun calcValue(evaluationContext: EvaluationContextImpl?): Value? {
        return computedValueFromGetter
    }

    override fun calcValueName(): String? {
        return with (StringBuilder()) {
            val classRenderer = NodeRendererSettings.getInstance()?.getClassRenderer()!!
            append(getName())
            if (classRenderer.SHOW_DECLARED_TYPE) {
                append(": ")
                append(classRenderer.renderTypeName(getValue()?.type()?.name()))
            }
            toString()
        }
    }

    override fun getName(): String {
        return delegate.name().trimTrailing(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)
    }

    override fun getDescriptorEvaluation(context: DebuggerContext?): PsiExpression? {
        return null
    }

}

