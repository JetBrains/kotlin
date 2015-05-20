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

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.impl.PositionUtil
import com.intellij.debugger.ui.breakpoints.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import org.jetbrains.annotations.TestOnly
import org.jetbrains.java.debugger.breakpoints.properties.JavaFieldBreakpointProperties
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
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
                if (descriptor is PropertyDescriptor && bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor)) {
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
        return delegate.createBreakpointProperties(file, line)
    }

    override fun addBreakpoint(project: Project?, parentComponent: JComponent?): XLineBreakpoint<JavaFieldBreakpointProperties>? {
        return delegate.addBreakpoint(project, parentComponent)
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
        return delegate.createProperties()
    }

    override fun createCustomPropertiesPanel(): XBreakpointCustomPropertiesPanel<XLineBreakpoint<JavaFieldBreakpointProperties>>? {
        return delegate.createCustomPropertiesPanel()
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

class KotlinFieldBreakpoint(project: Project, breakpoint: XBreakpoint<JavaFieldBreakpointProperties>): FieldBreakpoint(project, breakpoint) {
    override fun isValid(): Boolean {
        if (!BreakpointWithHighlighter.isPositionValid(getXBreakpoint().getSourcePosition())) return false

        return runReadAction {
            val field = getField()
            field != null && field.isValid()
        }
    }

    public fun getField(): JetCallableDeclaration? {
        val sourcePosition = getSourcePosition()
        return getProperty(sourcePosition)
    }

    private fun getProperty(sourcePosition: SourcePosition?): JetCallableDeclaration? {
        val property: JetProperty? = PositionUtil.getPsiElementAt(getProject(), javaClass(), sourcePosition)
        if (property != null) {
            return property
        }
        val parameter: JetParameter? = PositionUtil.getPsiElementAt(getProject(), javaClass(), sourcePosition)
        if (parameter != null) {
            return parameter
        }
        return null
    }

    override fun reload(psiFile: PsiFile?) {
        val property = getProperty(getSourcePosition())
        if (property != null) {
            setFieldName(property.getName())

            if (property is JetProperty && property.isTopLevel()) {
                getProperties().myClassName = PackageClassUtils.getPackageClassFqName(property.getContainingJetFile().getPackageFqName()).asString()
            }
            else {
                val jetClass: JetClassOrObject? = PsiTreeUtil.getParentOfType(property, javaClass())
                if (jetClass is JetClassOrObject) {
                    val fqName = jetClass.getFqName()
                    if (fqName != null) {
                        getProperties().myClassName = fqName.asString()
                    }
                }
            }
            setInstanceFiltersEnabled(false)
        }
    }

    fun setFieldName(fieldName: String) {
        getProperties().myFieldName = fieldName
    }

    @TestOnly
    fun setWatchAccess(value: Boolean) {
        getProperties().WATCH_ACCESS = value
    }

    @TestOnly
    fun setWatchModification(value: Boolean) {
        getProperties().WATCH_MODIFICATION = value
    }
}