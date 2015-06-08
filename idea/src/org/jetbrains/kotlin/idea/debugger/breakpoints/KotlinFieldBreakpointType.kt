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
        return delegate.createBreakpointProperties(file, line)
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
                    JavaFieldBreakpointProperties(fieldName, className)
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

class KotlinFieldBreakpoint(project: Project, breakpoint: XBreakpoint<JavaFieldBreakpointProperties>) : FieldBreakpoint(project, breakpoint) {
    companion object {
        private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.debugger.breakpoints.KotlinFieldBreakpoint")
    }

    private enum class BreakpointType {
        FIELD,
        METHOD
    }

    private val breakpointType: BreakpointType

    init {
        val element = getField()!!
        val bindingContext = element.analyzeAndGetResult().bindingContext
        var descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
        if (descriptor is ValueParameterDescriptor) {
            descriptor = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor)
        }

        breakpointType = if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor as PropertyDescriptor)) {
            BreakpointType.FIELD
        }
        else {
            BreakpointType.METHOD
        }
    }

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

    override fun createRequestForPreparedClass(debugProcess: DebugProcessImpl?, refType: ReferenceType?) {
        if (debugProcess == null || refType == null) return

        val vm = debugProcess.getVirtualMachineProxy()
        try {
            when (breakpointType) {
                BreakpointType.FIELD -> {
                    val field = refType.fieldByName(getFieldName())
                    if (field != null) {
                        val manager = debugProcess.getRequestsManager()
                        if (getProperties().WATCH_MODIFICATION && vm.canWatchFieldModification()) {
                            val request = manager.createModificationWatchpointRequest(this, field)
                            debugProcess.getRequestsManager().enableRequest(request)
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Modification request added")
                            }
                        }
                        if (getProperties().WATCH_ACCESS && vm.canWatchFieldAccess()) {
                            val request = manager.createAccessWatchpointRequest(this, field)
                            debugProcess.getRequestsManager().enableRequest(request)
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Field access request added (field = ${field.name()}; refType = ${refType.name()})")
                            }
                        }
                    }
                }
                BreakpointType.METHOD -> {
                    val propertyName = Name.identifier(getFieldName())

                    if (getProperties().WATCH_ACCESS) {
                        val getter = refType.methodsByName(PropertyCodegen.getterName(propertyName)).firstOrNull()
                        if (getter != null) {
                            createMethodBreakpoint(debugProcess, refType, getter)
                        }
                    }

                    if (getProperties().WATCH_MODIFICATION) {
                        val setter = refType.methodsByName(PropertyCodegen.setterName(propertyName)).firstOrNull()
                        if (setter != null) {
                            createMethodBreakpoint(debugProcess, refType, setter)
                        }
                    }
                }
            }
        }
        catch (ex: Exception) {
            LOG.debug(ex)
        }
    }

    private fun createMethodBreakpoint(debugProcess: DebugProcessImpl, refType: ReferenceType, accessor: Method) {
        val manager = debugProcess.getRequestsManager()
        val line = accessor.allLineLocations().firstOrNull()
        if (line != null) {
            val request = manager.createBreakpointRequest(this, line)
            debugProcess.getRequestsManager().enableRequest(request)
            if (LOG.isDebugEnabled()) {
                LOG.debug("Breakpoint request added")
            }
        }
        else {
            var entryRequest: MethodEntryRequest? = findRequest(debugProcess, javaClass(), this)
            if (entryRequest == null) {
                entryRequest = manager.createMethodEntryRequest(this)!!
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Method entry request added (method = ${accessor.name()}; refType = ${refType.name()})")
                }
            }
            else {
                entryRequest.disable()
            }
            entryRequest.addClassFilter(refType)
            manager.enableRequest(entryRequest)
        }
    }

    inline fun <reified T : EventRequest> findRequest(debugProcess: DebugProcessImpl, requestClass: Class<T>, requestor: Requestor): T? {
        val requests = debugProcess.getRequestsManager().findRequests(requestor)
        for (eventRequest in requests) {
            if (eventRequest.javaClass == requestClass) {
                return eventRequest as T
            }
        }
        return null
    }

    override fun evaluateCondition(context: EvaluationContextImpl, event: LocatableEvent): Boolean {
        if (breakpointType == BreakpointType.METHOD && !matchesEvent(event)) {
            return false
        }
        return super.evaluateCondition(context, event)
    }

    public fun matchesEvent(event: LocatableEvent): Boolean {
        val method = event.location().method()
        // TODO check property type
        return method != null && method.name() in getMethodsName()
    }

    private fun getMethodsName(): List<String> {
        val propertyName = Name.identifier(getFieldName())
        return arrayListOf(PropertyCodegen.getterName(propertyName), PropertyCodegen.setterName(propertyName))

    }

    override fun getEventMessage(event: LocatableEvent?): String? {
        return super.getEventMessage(event)
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