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

import com.intellij.debugger.ui.tree.render.ClassRenderer
import com.sun.jdi.Type as JdiType
import com.sun.jdi.Value
import com.intellij.debugger.ui.tree.render.ChildrenBuilder
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.ui.tree.DebuggerTreeNode
import org.jetbrains.org.objectweb.asm.Type as AsmType
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.sun.jdi.ObjectReference
import com.intellij.xdebugger.settings.XDebuggerSettingsManager
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl
import com.intellij.debugger.ui.impl.watch.MessageDescriptor
import java.util.ArrayList
import org.jetbrains.kotlin.load.java.JvmAbi
import com.sun.jdi.Field
import com.sun.jdi.ReferenceType
import com.sun.jdi.Type
import com.sun.jdi.Method
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.name.Name
import com.intellij.debugger.engine.evaluation.EvaluateException
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings

public class KotlinClassWithDelegatedPropertyRenderer : ClassRenderer() {

    override fun isApplicable(jdiType: Type?): Boolean {
        if (!super.isApplicable(jdiType)) return false

        if (jdiType !is ReferenceType) return false

        return jdiType.allFields().any { it.name().endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX) }
    }

    override fun buildChildren(value: Value?, builder: ChildrenBuilder, context: EvaluationContext) {
        DebuggerManagerThreadImpl.assertIsManagerThread()

        if (value !is ObjectReference) return

        val nodeManager = builder.getNodeManager()!!
        val nodeDescriptorFactory = builder.getDescriptorManager()!!

        val fields = value.referenceType().allFields()
        if (fields.isEmpty()) {
            builder.setChildren(listOf(nodeManager.createMessageNode(MessageDescriptor.CLASS_HAS_NO_FIELDS.getLabel())))
            return
        }

        val children = ArrayList<DebuggerTreeNode>()
        for (field in fields) {
            if (!shouldDisplay(context, value, field)) {
                continue
            }

            val fieldDescriptor = nodeDescriptorFactory.getFieldDescriptor(builder.getParentDescriptor(), value, field)

            if (field.name().endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)) {
                val shouldRenderDelegatedProperty = KotlinDebuggerSettings.getInstance().DEBUG_RENDER_DELEGATED_PROPERTIES
                if (shouldRenderDelegatedProperty) {
                    children.add(nodeManager.createNode(fieldDescriptor, context))
                }

                val delegatedPropertyDescriptor = DelegatedPropertyFieldDescriptor(
                        context.getDebugProcess().getProject()!!,
                        value,
                        field,
                        shouldRenderDelegatedProperty)
                children.add(nodeManager.createNode(delegatedPropertyDescriptor, context))
            }
            else {
                children.add(nodeManager.createNode(fieldDescriptor, context))
            }
        }

        if (XDebuggerSettingsManager.getInstance()!!.getDataViewSettings().isSortValues()) {
            children.sortBy(NodeManagerImpl.getNodeComparator())
        }

        builder.setChildren(children)
    }

}
