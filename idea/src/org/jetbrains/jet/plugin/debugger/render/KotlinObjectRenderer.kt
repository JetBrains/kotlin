/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger.render

import com.intellij.debugger.ui.tree.render.ClassRenderer
import com.sun.jdi.Type
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.sun.jdi.ObjectReference
import com.sun.jdi.Field
import com.sun.jdi.ClassType
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.ui.tree.NodeDescriptorFactory
import com.intellij.debugger.ui.tree.FieldDescriptor
import com.intellij.debugger.ui.impl.watch.FieldDescriptorImpl
import com.intellij.openapi.project.Project
import com.intellij.debugger.impl.DebuggerContextImpl
import org.jetbrains.jet.lang.resolve.java.JvmAbi
import com.intellij.debugger.ui.tree.ValueDescriptor
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.debugger.settings.NodeRendererSettings

public class KotlinObjectRenderer : ClassRenderer() {

    override fun isApplicable(jdiType: Type?): Boolean {
        if (!super.isApplicable(jdiType)) return false

        return jdiType.isKotlinClass()
    }

    override fun createFieldDescriptor(
            parentDescriptor: ValueDescriptorImpl?,
            nodeDescriptorFactory: NodeDescriptorFactory?,
            objRef: ObjectReference?,
            field: Field?,
            evaluationContext: EvaluationContext?
    ): FieldDescriptor {
        if (field?.declaringType().isKotlinClass()) {
            return KotlinObjectFieldDescriptor(evaluationContext?.getProject(), objRef, field)
        }
        return super.createFieldDescriptor(parentDescriptor, nodeDescriptorFactory, objRef, field, evaluationContext)
    }

    override fun calcLabel(
            descriptor: ValueDescriptor?,
            evaluationContext: EvaluationContext?,
            labelListener: DescriptorLabelListener?
    ): String? {
        val toStringRenderer = NodeRendererSettings.getInstance().getToStringRenderer()
        if (toStringRenderer.isApplicable(descriptor?.getValue()?.type())) {
            return toStringRenderer.calcLabel(descriptor, evaluationContext, labelListener)
        }
        return super.calcLabel(descriptor, evaluationContext, labelListener)
    }

    private fun Type?.isKotlinClass(): Boolean {
        return this is ClassType && this.allInterfaces().any { it.name() == JvmAbi.K_OBJECT.asString() }
    }
}

public class KotlinObjectFieldDescriptor(
        project: Project?,
        objRef: ObjectReference?,
        field: Field?
): FieldDescriptorImpl(project, objRef, field) {
    override fun getSourcePosition(project: Project?, context: DebuggerContextImpl?) = null
    override fun getSourcePosition(project: Project?, context: DebuggerContextImpl?, nearest: Boolean) = null
}

