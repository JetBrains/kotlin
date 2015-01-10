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

package org.jetbrains.jet.plugin.debugger

import com.intellij.debugger.engine.SourcePositionProvider
import com.intellij.debugger.ui.tree.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.SourcePosition
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.psi.JetClassOrObject
import com.intellij.debugger.impl.DebuggerContextUtil
import com.sun.jdi.ReferenceType
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ClassNotPreparedException
import com.intellij.debugger.ui.tree.FieldDescriptor

public class KotlinSourcePositionProvider: SourcePositionProvider() {
    override fun computeSourcePosition(descriptor: NodeDescriptor, project: Project, context: DebuggerContextImpl, nearest: Boolean): SourcePosition? {
        if (context.getFrameProxy() == null) return null

        if (descriptor is FieldDescriptor) {
            return computeSourcePosition(descriptor, project, context, nearest)
        }

        return null
    }

    fun computeSourcePosition(descriptor: FieldDescriptor, project: Project, context: DebuggerContextImpl, nearest: Boolean): SourcePosition? {
        val fieldName = descriptor.getField().name()
        if (fieldName == AsmUtil.CAPTURED_THIS_FIELD || fieldName == AsmUtil.CAPTURED_RECEIVER_FIELD) {
            return null
        }

        val type = descriptor.getField().declaringType()
        val myClass = findClassByType(project, type, context)?.getNavigationElement()
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

    private fun findClassByType(project: Project, type: ReferenceType, context: DebuggerContextImpl): PsiElement? {
        val session = context.getDebuggerSession()
        val scope = if (session != null) session.getSearchScope() else GlobalSearchScope.allScope(project)
        val className = JvmClassName.byInternalName(type.name()).getFqNameForClassNameWithoutDollars().asString()

        val myClass = JavaPsiFacade.getInstance(project).findClass(className, scope)
        if (myClass != null) return myClass

        val position = getLastSourcePosition(type, context)
        if (position != null) {
            val element = position.getElementAt()
            if (element != null) {
                return element.getStrictParentOfType<JetClassOrObject>()
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
