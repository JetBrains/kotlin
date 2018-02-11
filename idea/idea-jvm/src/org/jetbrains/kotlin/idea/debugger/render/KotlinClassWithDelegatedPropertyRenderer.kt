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

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.impl.watch.MessageDescriptor
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl
import com.intellij.debugger.ui.tree.DebuggerTreeNode
import com.intellij.debugger.ui.tree.ValueDescriptor
import com.intellij.debugger.ui.tree.render.ChildrenBuilder
import com.intellij.debugger.ui.tree.render.ClassRenderer
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.settings.XDebuggerSettingsManager
import com.sun.jdi.*
import com.sun.jdi.Type
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerSettings
import org.jetbrains.kotlin.load.java.JvmAbi
import java.util.*
import com.sun.jdi.Type as JdiType
import org.jetbrains.org.objectweb.asm.Type as AsmType

private val LOG = Logger.getInstance(KotlinClassWithDelegatedPropertyRenderer::class.java)
private fun notPreparedClassMessage(referenceType: ReferenceType) =
    "$referenceType ${referenceType.isPrepared} ${referenceType.sourceName()}"

class KotlinClassWithDelegatedPropertyRenderer(private val rendererSettings: NodeRendererSettings) : ClassRenderer() {
    override fun isApplicable(jdiType: Type?): Boolean {
        if (!super.isApplicable(jdiType)) return false

        if (jdiType !is ReferenceType) return false

        if (!jdiType.isPrepared) {
            LOG.info(notPreparedClassMessage(jdiType))
            return false
        }

        try {
            return jdiType.allFields().any { it.name().endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX) }
        }
        catch (notPrepared: ClassNotPreparedException) {
            LOG.error(notPreparedClassMessage(jdiType), notPrepared)
        }

        return false
    }

    override fun calcLabel(descriptor: ValueDescriptor,
                           evaluationContext: EvaluationContext,
                           listener: DescriptorLabelListener): String {
        val res = calcToStringLabel(descriptor, evaluationContext, listener)
        if (res != null) {
            return res
        }

        return super.calcLabel(descriptor, evaluationContext, listener)
    }

    private fun calcToStringLabel(descriptor: ValueDescriptor, evaluationContext: EvaluationContext,
                                  listener: DescriptorLabelListener): String? {
        val toStringRenderer = rendererSettings.toStringRenderer
        if (toStringRenderer.isEnabled && DebuggerManagerEx.getInstanceEx(evaluationContext.project).context.isEvaluationPossible) {
            if (toStringRenderer.isApplicable(descriptor.type)) {
                return toStringRenderer.calcLabel(descriptor, evaluationContext, listener)
            }
        }
        return null
    }

    override fun buildChildren(value: Value?, builder: ChildrenBuilder, context: EvaluationContext) {
        DebuggerManagerThreadImpl.assertIsManagerThread()

        if (value !is ObjectReference) return

        val nodeManager = builder.nodeManager!!
        val nodeDescriptorFactory = builder.descriptorManager!!

        val fields = value.referenceType().allFields()
        if (fields.isEmpty()) {
            builder.setChildren(listOf(nodeManager.createMessageNode(MessageDescriptor.CLASS_HAS_NO_FIELDS.label)))
            return
        }

        val children = ArrayList<DebuggerTreeNode>()
        for (field in fields) {
            if (!shouldDisplay(context, value, field)) {
                continue
            }

            val fieldDescriptor = nodeDescriptorFactory.getFieldDescriptor(builder.parentDescriptor, value, field)

            if (field.name().endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX)) {
                val shouldRenderDelegatedProperty = KotlinDebuggerSettings.getInstance().DEBUG_RENDER_DELEGATED_PROPERTIES
                if (shouldRenderDelegatedProperty) {
                    children.add(nodeManager.createNode(fieldDescriptor, context))
                }

                val delegatedPropertyDescriptor = DelegatedPropertyFieldDescriptor(
                        context.debugProcess.project!!,
                        value,
                        field,
                        shouldRenderDelegatedProperty)
                children.add(nodeManager.createNode(delegatedPropertyDescriptor, context))
            }
            else {
                children.add(nodeManager.createNode(fieldDescriptor, context))
            }
        }

        if (XDebuggerSettingsManager.getInstance()!!.dataViewSettings.isSortValues) {
            children.sortedWith(NodeManagerImpl.getNodeComparator())
        }

        builder.setChildren(children)
    }

}
