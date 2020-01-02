/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.render

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.ui.impl.watch.FieldDescriptorImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiExpression
import com.sun.jdi.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name

class DelegatedPropertyFieldDescriptor(
    project: Project,
    objectRef: ObjectReference,
    val delegate: Field,
    private val renderDelegatedProperty: Boolean
) : FieldDescriptorImpl(project, objectRef, delegate) {

    override fun calcValue(evaluationContext: EvaluationContextImpl?): Value? {
        if (evaluationContext == null) return null
        if (!renderDelegatedProperty) return super.calcValue(evaluationContext)

        val method = findGetterForDelegatedProperty()
        val threadReference = evaluationContext.suspendContext.thread?.threadReference
        if (method == null || threadReference == null) {
            return super.calcValue(evaluationContext)
        }

        return try {
            evaluationContext.debugProcess.invokeInstanceMethod(
                evaluationContext,
                `object`,
                method,
                listOf<Nothing>(),
                evaluationContext.suspendContext.suspendPolicy
            )
        } catch (e: EvaluateException) {
            e.exceptionFromTargetVM
        }
    }

    override fun getName(): String {
        return delegate.name().removeSuffix(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)
    }

    override fun getDescriptorEvaluation(context: DebuggerContext?): PsiExpression? {
        return null
    }

    private fun findGetterForDelegatedProperty(): Method? {
        val fieldName = name
        if (!Name.isValidIdentifier(fieldName)) return null

        return `object`.referenceType().methodsByName(JvmAbi.getterName(fieldName))?.firstOrNull()
    }

    override fun getDeclaredType(): String? {
        val getter = findGetterForDelegatedProperty() ?: return null
        val returnType = try {
            getter.returnType()
        } catch (e: ClassNotLoadedException) {
            // Behavior copied from LocalVariableDescriptorImpl (in platform)
            return "<unknown>"
        }
        return returnType?.name()
    }
}
