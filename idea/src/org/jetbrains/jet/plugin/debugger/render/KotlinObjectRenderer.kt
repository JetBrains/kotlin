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
import com.intellij.psi.PsiClass
import com.intellij.psi.JavaPsiFacade
import com.intellij.debugger.SourcePosition
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.debugger.impl.DebuggerContextUtil
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ClassNotPreparedException
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import com.sun.jdi.ReferenceType
import org.jetbrains.jet.codegen.AsmUtil
import org.jetbrains.jet.lang.psi.JetClassOrObject

public open class KotlinObjectRenderer : ClassRenderer() {

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
}

public class KotlinObjectFieldDescriptor(
        project: Project?,
        objRef: ObjectReference?,
        field: Field?
) : FieldDescriptorImpl(project, objRef, field) {
    override fun getSourcePosition(project: Project?, context: DebuggerContextImpl?, nearest: Boolean): SourcePosition? {
        if (context == null || context.getFrameProxy() == null) return null

        val fieldName = getField().name()
        if (fieldName == AsmUtil.CAPTURED_THIS_FIELD || fieldName == AsmUtil.CAPTURED_RECEIVER_FIELD) {
            return null
        }

        val type = getField().declaringType()
        val myClass = findClassByType(type, context)?.getNavigationElement()
        if (myClass !is JetClassOrObject) {
            return null
        }

        val field = myClass.getDeclarations().firstOrNull { fieldName == it.getName() }
        if (field == null) return null

        if (nearest) {
            return DebuggerContextUtil.findNearest(context, field, myClass.getContainingFile())
        }
        return SourcePosition.createFromOffset(field.getContainingFile(), field.getTextOffset())
    }

    private fun findClassByType(type: ReferenceType, context: DebuggerContextImpl): PsiElement? {
        val session = context.getDebuggerSession()
        val scope = if (session != null) session.getSearchScope() else GlobalSearchScope.allScope(myProject)
        val className = JvmClassName.byInternalName(type.name()).getFqNameForClassNameWithoutDollars().asString()

        val myClass = JavaPsiFacade.getInstance(myProject).findClass(className, scope)
        if (myClass != null) return myClass

        val position = getLastSourcePosition(type, context)
        if (position != null) {
            val element = position.getElementAt()
            if (element != null) {
                return PsiTreeUtil.getParentOfType(element, javaClass<JetClassOrObject>())
            }
        }
        return null
    }


    private fun getLastSourcePosition(type: ReferenceType, context: DebuggerContextImpl): SourcePosition? {
        val debugProcess = context.getDebugProcess()
        if (debugProcess != null) {
            try {
                val locations = type.allLineLocations()
                if (!locations.isEmpty()) {
                    val lastLocation = locations.get(locations.size() - 1)
                    return debugProcess.getPositionManager().getSourcePosition(lastLocation)
                }
            }
            catch (ignored: AbsentInformationException) {
            }
            catch (ignored: ClassNotPreparedException) {
            }
        }
        return null
    }
}

private fun Type?.isKotlinClass(): Boolean {
    return this is ClassType && this.allInterfaces().any { it.name() == JvmAbi.K_OBJECT.asString() }
}

