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

package org.jetbrains.kotlin.idea.debugger.render

import com.intellij.openapi.project.Project
import com.sun.jdi.Field
import org.jetbrains.kotlin.load.java.JvmAbi
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.sun.jdi.Value
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.DebuggerContext
import com.intellij.psi.PsiExpression
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.tree.FieldDescriptor
import com.sun.jdi.ObjectReference

class DelegatedPropertyFieldDescriptor(
        project: Project,
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

