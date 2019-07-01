/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.BreakpointManager
import com.intellij.debugger.ui.breakpoints.BreakpointWithHighlighter
import com.intellij.debugger.ui.breakpoints.JavaBreakpointType
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.breakpoints.dialog.AddFieldBreakpointDialog
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import javax.swing.JComponent

class KotlinFieldBreakpointType : JavaBreakpointType<KotlinPropertyBreakpointProperties>, XLineBreakpointType<KotlinPropertyBreakpointProperties>(
        "kotlin-field", KotlinBundle.message("debugger.field.watchpoints.tab.title")
) {
    override fun createJavaBreakpoint(project: Project, breakpoint: XBreakpoint<KotlinPropertyBreakpointProperties>): Breakpoint<KotlinPropertyBreakpointProperties> {
        return KotlinFieldBreakpoint(project, breakpoint)
    }

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        return canPutAt(file, line, project, this::class.java)
    }

    override fun getPriority() = 120

    override fun createBreakpointProperties(file: VirtualFile, line: Int): KotlinPropertyBreakpointProperties? {
        return KotlinPropertyBreakpointProperties()
    }

    override fun addBreakpoint(project: Project, parentComponent: JComponent?): XLineBreakpoint<KotlinPropertyBreakpointProperties>? {
        var result: XLineBreakpoint<KotlinPropertyBreakpointProperties>? = null

        val dialog = object : AddFieldBreakpointDialog(project) {
            override fun validateData(): Boolean {
                val className = className
                if (className.isEmpty()) {
                    reportError(project, DebuggerBundle.message("error.field.breakpoint.class.name.not.specified"))
                    return false
                }

                val psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
                if (psiClass !is KtLightClass) {
                    reportError(project, "Couldn't find '$className' class")
                    return false
                }

                val fieldName = fieldName
                if (fieldName.isEmpty()) {
                    reportError(project, DebuggerBundle.message("error.field.breakpoint.field.name.not.specified"))
                    return false
                }

                result = when (psiClass) {
                    is KtLightClassForFacade -> {
                        psiClass.files.asSequence().mapNotNull { createBreakpointIfPropertyExists(it, it, className, fieldName) }.firstOrNull()
                    }
                    is KtLightClassForSourceDeclaration -> {
                        val jetClass = psiClass.kotlinOrigin
                        createBreakpointIfPropertyExists(jetClass, jetClass.containingKtFile, className, fieldName)
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
            declaration: KtDeclarationContainer,
            file: KtFile,
            className: String,
            fieldName: String
    ): XLineBreakpoint<KotlinPropertyBreakpointProperties>? {
        val project = file.project
        val property = declaration.declarations.firstOrNull { it is KtProperty && it.name == fieldName } ?: return null

        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return null
        val line = document.getLineNumber(property.textOffset)
        return runWriteAction {
            XDebuggerManager.getInstance(project).breakpointManager.addLineBreakpoint(
                    this,
                    file.virtualFile.url,
                    line,
                    KotlinPropertyBreakpointProperties(fieldName, className)
            )
        }
    }

    private fun reportError(project: Project, message: String) {
        Messages.showMessageDialog(project, message, DebuggerBundle.message("add.field.breakpoint.dialog.title"), Messages.getErrorIcon())
    }

    override fun isAddBreakpointButtonVisible() = true

    override fun getMutedEnabledIcon() = AllIcons.Debugger.Db_muted_field_breakpoint

    override fun getDisabledIcon() = AllIcons.Debugger.Db_disabled_field_breakpoint

    override fun getEnabledIcon() = AllIcons.Debugger.Db_field_breakpoint

    override fun getMutedDisabledIcon() = AllIcons.Debugger.Db_muted_disabled_field_breakpoint

    override fun canBeHitInOtherPlaces() = true

    override fun getShortText(breakpoint: XLineBreakpoint<KotlinPropertyBreakpointProperties>): String? {
        val properties = breakpoint.properties
        val className = properties.myClassName
        return if (!className.isEmpty()) className + "." + properties.myFieldName else properties.myFieldName
    }

    override fun createProperties(): KotlinPropertyBreakpointProperties? {
        return KotlinPropertyBreakpointProperties()
    }

    override fun createCustomPropertiesPanel(): XBreakpointCustomPropertiesPanel<XLineBreakpoint<KotlinPropertyBreakpointProperties>>? {
        return KotlinFieldBreakpointPropertiesPanel()
    }

    override fun getDisplayText(breakpoint: XLineBreakpoint<KotlinPropertyBreakpointProperties>): String? {
        val kotlinBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint) as? BreakpointWithHighlighter
        return if (kotlinBreakpoint != null) {
            kotlinBreakpoint.description
        }
        else {
            super.getDisplayText(breakpoint)
        }
    }

    override fun getEditorsProvider() = null

    override fun createCustomRightPropertiesPanel(project: Project): XBreakpointCustomPropertiesPanel<XLineBreakpoint<KotlinPropertyBreakpointProperties>>? {
        return KotlinBreakpointFiltersPanel(project)
    }

    override fun isSuspendThreadSupported() = true
}