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

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.PositionUtil
import com.intellij.debugger.requests.Requestor
import com.intellij.debugger.ui.breakpoints.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Method
import com.sun.jdi.ObjectCollectedException
import com.sun.jdi.ReferenceType
import com.sun.jdi.event.*
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.MethodEntryRequest
import org.jetbrains.annotations.TestOnly
import org.jetbrains.java.debugger.breakpoints.properties.JavaFieldBreakpointProperties
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.asJava.KotlinLightClassForExplicitDeclaration
import org.jetbrains.kotlin.asJava.KotlinLightClassForPackage
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import javax.swing.Icon
import javax.swing.JComponent

public class KotlinFieldBreakpointType : JavaBreakpointType<JavaFieldBreakpointProperties>, XLineBreakpointType<JavaFieldBreakpointProperties>(
        "kotlin-field", JetBundle.message("debugger.field.watchpoints.tab.title")
) {
    private val delegate = JavaFieldBreakpointType()

    override fun createJavaBreakpoint(project: Project, breakpoint: XBreakpoint<JavaFieldBreakpointProperties>): Breakpoint<*> {
        return KotlinFieldBreakpoint(project, breakpoint)
    }

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        val psiFile = PsiManager.getInstance(project).findFile(file)

        if (psiFile == null || psiFile.getVirtualFile().getFileType() != JetFileType.INSTANCE) {
            return false
        }

        val document = FileDocumentManager.getInstance().getDocument(file) ?: return false

        var canPutAt = false
        XDebuggerUtil.getInstance().iterateLine(project, document, line, fun (el: PsiElement): Boolean {
            // avoid comments
            if (el is PsiWhiteSpace || PsiTreeUtil.getParentOfType(el, javaClass<PsiComment>(), false) != null) {
                return true
            }

            var element = el
            var parent = element.getParent()
            while (parent != null) {
                val offset = parent.getTextOffset()
                if (offset >= 0 && document.getLineNumber(offset) != line) break

                element = parent
                parent = element.getParent()
            }

            if (element is JetProperty || element is JetParameter) {
                val bindingContext = (element as JetElement).analyzeAndGetResult().bindingContext
                var descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
                if (descriptor is ValueParameterDescriptor) {
                    descriptor = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor)
                }
                if (descriptor is PropertyDescriptor) {
                    canPutAt = true
                }
                return false
            }

            return true
        })

        return canPutAt
    }

    override fun getPriority() = 120

    override fun createBreakpointProperties(file: VirtualFile, line: Int): JavaFieldBreakpointProperties? {
        return KotlinPropertyBreakpointProperties()
    }

    override fun addBreakpoint(project: Project, parentComponent: JComponent?): XLineBreakpoint<JavaFieldBreakpointProperties>? {
        var result: XLineBreakpoint<JavaFieldBreakpointProperties>? = null

        val dialog = object : AddFieldBreakpointDialog(project) {
            override fun validateData(): Boolean {
                val className = getClassName()
                if (className.isEmpty()) {
                    reportError(project, DebuggerBundle.message("error.field.breakpoint.class.name.not.specified"))
                    return false
                }

                val psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
                if (psiClass !is KotlinLightClass) {
                    reportError(project, "Couldn't find '$className' class")
                    return false
                }

                val fieldName = getFieldName()
                if (fieldName.isEmpty()) {
                    reportError(project, DebuggerBundle.message("error.field.breakpoint.field.name.not.specified"))
                    return false
                }

                result = when (psiClass) {
                    is KotlinLightClassForPackage -> {
                        psiClass.files.asSequence().map { createBreakpointIfPropertyExists(it, it, className, fieldName) }.firstOrNull()
                    }
                    is KotlinLightClassForExplicitDeclaration -> {
                        val jetClass = psiClass.getOrigin()
                        createBreakpointIfPropertyExists(jetClass, jetClass.getContainingJetFile(), className, fieldName)
                    }
                    else -> null
                }

                if (result == null) {
                    reportError(project, DebuggerBundle.message("error.field.breakpoint.field.not.found", className, fieldName, fieldName))
                }

                return result != null
            }
        }

        dialog.show()
        return result
    }

    private fun createBreakpointIfPropertyExists(
            declaration: JetDeclarationContainer,
            file: JetFile,
            className: String,
            fieldName: String
    ): XLineBreakpoint<JavaFieldBreakpointProperties>? {
        val project = file.getProject()
        val property = declaration.getDeclarations().firstOrNull { it is JetProperty && it.getName() == fieldName } ?: return null

        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return null
        val line = document.getLineNumber(property.getTextOffset())
        return runWriteAction {
            XDebuggerManager.getInstance(project).getBreakpointManager().addLineBreakpoint(
                    this,
                    file.getVirtualFile().getUrl(),
                    line,
                    KotlinPropertyBreakpointProperties(fieldName, className)
            )
        }
    }

    private fun reportError(project: Project, message: String) {
        Messages.showMessageDialog(project, message, DebuggerBundle.message("add.field.breakpoint.dialog.title"), Messages.getErrorIcon())
    }

    override fun isAddBreakpointButtonVisible(): Boolean {
        return delegate.isAddBreakpointButtonVisible()
    }

    override fun getMutedEnabledIcon(): Icon {
        return delegate.getMutedEnabledIcon()
    }

    override fun getDisabledIcon(): Icon {
        return delegate.getDisabledIcon()
    }

    override fun getEnabledIcon(): Icon {
        return delegate.getEnabledIcon()
    }

    override fun getMutedDisabledIcon(): Icon {
        return delegate.getMutedDisabledIcon()
    }

    override fun canBeHitInOtherPlaces(): Boolean {
        return delegate.canBeHitInOtherPlaces()
    }

    override fun getShortText(breakpoint: XLineBreakpoint<JavaFieldBreakpointProperties>?): String? {
        return delegate.getShortText(breakpoint)
    }

    override fun createProperties(): JavaFieldBreakpointProperties? {
        return KotlinPropertyBreakpointProperties()
    }

    override fun createCustomPropertiesPanel(): XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaFieldBreakpointProperties>>? {
        return KotlinFieldBreakpointPropertiesPanel() as XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaFieldBreakpointProperties>>
    }

    override fun getDisplayText(breakpoint: XLineBreakpoint<JavaFieldBreakpointProperties>?): String? {
        return delegate.getDisplayText(breakpoint)
    }

    override fun getEditorsProvider(): XDebuggerEditorsProvider? {
        return delegate.getEditorsProvider()
    }

    override fun createCustomRightPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaFieldBreakpointProperties>>? {
        return delegate.createCustomRightPropertiesPanel(project)
    }

    override fun isSuspendThreadSupported(): Boolean {
        return delegate.isSuspendThreadSupported()
    }
}