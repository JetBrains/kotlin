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
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.PositionUtil
import com.intellij.debugger.requests.Requestor
import com.intellij.debugger.ui.breakpoints.BreakpointCategory
import com.intellij.debugger.ui.breakpoints.BreakpointWithHighlighter
import com.intellij.debugger.ui.breakpoints.FieldBreakpoint
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Method
import com.sun.jdi.ReferenceType
import com.sun.jdi.event.*
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.MethodEntryRequest
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import javax.swing.Icon

class KotlinFieldBreakpoint(
        project: Project,
        breakpoint: XBreakpoint<KotlinPropertyBreakpointProperties>
): BreakpointWithHighlighter<KotlinPropertyBreakpointProperties>(project, breakpoint) {
    companion object {
        private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.debugger.breakpoints.KotlinFieldBreakpoint")
        private val CATEGORY: Key<FieldBreakpoint> = BreakpointCategory.lookup<FieldBreakpoint>("field_breakpoints")
    }

    private enum class BreakpointType {
        FIELD,
        METHOD
    }

    private var breakpointType: BreakpointType = BreakpointType.FIELD

    override fun isValid(): Boolean {
        if (!BreakpointWithHighlighter.isPositionValid(xBreakpoint.sourcePosition)) return false

        return runReadAction {
            val field = getField()
            field != null && field.isValid
        }
    }

    fun getField(): KtCallableDeclaration? {
        val sourcePosition = sourcePosition
        return getProperty(sourcePosition)
    }

    private fun getProperty(sourcePosition: SourcePosition?): KtCallableDeclaration? {
        val property: KtProperty? = PositionUtil.getPsiElementAt(project, KtProperty::class.java, sourcePosition)
        if (property != null) {
            return property
        }
        val parameter: KtParameter? = PositionUtil.getPsiElementAt(project, KtParameter::class.java, sourcePosition)
        if (parameter != null) {
            return parameter
        }
        return null
    }

    override fun reload(psiFile: PsiFile?) {
        val property = getProperty(sourcePosition)
        if (property != null) {
            setFieldName(property.name!!)

            if (property is KtProperty && property.isTopLevel) {
                properties.myClassName = JvmFileClassUtil.getFileClassInfoNoResolve(property.getContainingKtFile()).fileClassFqName.asString()
            }
            else {
                val ktClass: KtClassOrObject? = PsiTreeUtil.getParentOfType(property, KtClassOrObject::class.java)
                if (ktClass is KtClassOrObject) {
                    val fqName = ktClass.fqName
                    if (fqName != null) {
                        properties.myClassName = fqName.asString()
                    }
                }
            }
            isInstanceFiltersEnabled = false
        }
    }

    override fun createRequestForPreparedClass(debugProcess: DebugProcessImpl?, refType: ReferenceType?) {
        if (debugProcess == null || refType == null) return

        val property = getProperty(sourcePosition) ?: return

        breakpointType = computeBreakpointType(property)

        val vm = debugProcess.virtualMachineProxy
        try {
            if (properties.WATCH_INITIALIZATION) {
                val sourcePosition = sourcePosition
                if (sourcePosition != null) {
                    debugProcess.positionManager
                            .locationsOfLine(refType, sourcePosition)
                            .filter { it.method().isConstructor || it.method().isStaticInitializer }
                            .forEach {
                                val request = debugProcess.requestsManager.createBreakpointRequest(this, it)
                                debugProcess.requestsManager.enableRequest(request)
                                if (LOG.isDebugEnabled) {
                                    LOG.debug("Breakpoint request added")
                                }
                            }
                }
            }
            
            when (breakpointType) {
                BreakpointType.FIELD -> {
                    val field = refType.fieldByName(getFieldName())
                    if (field != null) {
                        val manager = debugProcess.requestsManager
                        if (properties.WATCH_MODIFICATION && vm.canWatchFieldModification()) {
                            val request = manager.createModificationWatchpointRequest(this, field)
                            debugProcess.requestsManager.enableRequest(request)
                            if (LOG.isDebugEnabled) {
                                LOG.debug("Modification request added")
                            }
                        }
                        if (properties.WATCH_ACCESS && vm.canWatchFieldAccess()) {
                            val request = manager.createAccessWatchpointRequest(this, field)
                            debugProcess.requestsManager.enableRequest(request)
                            if (LOG.isDebugEnabled) {
                                LOG.debug("Field access request added (field = ${field.name()}; refType = ${refType.name()})")
                            }
                        }
                    }
                }
                BreakpointType.METHOD -> {
                    val fieldName = getFieldName()

                    if (properties.WATCH_ACCESS) {
                        val getter = refType.methodsByName(JvmAbi.getterName(fieldName)).firstOrNull()
                        if (getter != null) {
                            createMethodBreakpoint(debugProcess, refType, getter)
                        }
                    }

                    if (properties.WATCH_MODIFICATION) {
                        val setter = refType.methodsByName(JvmAbi.setterName(fieldName)).firstOrNull()
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

    private fun computeBreakpointType(property: KtCallableDeclaration): BreakpointType {
        return runReadAction {
            val bindingContext = property.analyze()
            var descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property)
            if (descriptor is ValueParameterDescriptor) {
                descriptor = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor)
            }

            if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor as PropertyDescriptor)!!) {
                BreakpointType.FIELD
            }
            else {
                BreakpointType.METHOD
            }
        }
    }

    private fun createMethodBreakpoint(debugProcess: DebugProcessImpl, refType: ReferenceType, accessor: Method) {
        val manager = debugProcess.requestsManager
        val line = accessor.allLineLocations().firstOrNull()
        if (line != null) {
            val request = manager.createBreakpointRequest(this, line)
            debugProcess.requestsManager.enableRequest(request)
            if (LOG.isDebugEnabled) {
                LOG.debug("Breakpoint request added")
            }
        }
        else {
            var entryRequest: MethodEntryRequest? = findRequest(debugProcess, MethodEntryRequest::class.java, this)
            if (entryRequest == null) {
                entryRequest = manager.createMethodEntryRequest(this)!!
                if (LOG.isDebugEnabled) {
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

    inline private fun <reified T : EventRequest> findRequest(debugProcess: DebugProcessImpl, requestClass: Class<T>, requestor: Requestor): T? {
        val requests = debugProcess.requestsManager.findRequests(requestor)
        for (eventRequest in requests) {
            if (eventRequest::class.java == requestClass) {
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

    fun matchesEvent(event: LocatableEvent): Boolean {
        val method = event.location().method()
        // TODO check property type
        return method != null && method.name() in getMethodsName()
    }

    private fun getMethodsName(): List<String> {
        val fieldName = getFieldName()
        return listOf(JvmAbi.getterName(fieldName), JvmAbi.setterName(fieldName))
    }

    override fun getEventMessage(event: LocatableEvent): String {
        val location = event.location()
        val locationQName = location.declaringType().name() + "." + location.method().name()
        val locationFileName = try {
            location.sourceName()
        }
        catch (e: AbsentInformationException) {
            fileName
        }
        catch (e: InternalError) {
            fileName
        }

        val locationLine = location.lineNumber()
        when (event) {
            is ModificationWatchpointEvent-> {
                val field = event.field()
                return DebuggerBundle.message(
                        "status.static.field.watchpoint.reached.access",
                        field.declaringType().name(),
                        field.name(),
                        locationQName,
                        locationFileName,
                        locationLine)
            }
            is AccessWatchpointEvent -> {
                val field = event.field()
                return DebuggerBundle.message(
                        "status.static.field.watchpoint.reached.access",
                        field.declaringType().name(),
                        field.name(),
                        locationQName,
                        locationFileName,
                        locationLine)
            }
            is MethodEntryEvent -> {
                val method = event.method()
                return DebuggerBundle.message(
                        "status.method.entry.breakpoint.reached",
                        method.declaringType().name() + "." + method.name() + "()",
                        locationQName,
                        locationFileName,
                        locationLine)
            }
            is MethodExitEvent -> {
                val method = event.method()
                return DebuggerBundle.message(
                        "status.method.exit.breakpoint.reached",
                        method.declaringType().name() + "." + method.name() + "()",
                        locationQName,
                        locationFileName,
                        locationLine)
            }
        }
        return DebuggerBundle.message(
                "status.line.breakpoint.reached",
                locationQName,
                locationFileName,
                locationLine)
    }

    fun setFieldName(fieldName: String) {
        properties.myFieldName = fieldName
    }

    @TestOnly
    fun setWatchAccess(value: Boolean) {
        properties.WATCH_ACCESS = value
    }

    @TestOnly
    fun setWatchModification(value: Boolean) {
        properties.WATCH_MODIFICATION = value
    }

    @TestOnly
    fun setWatchInitialization(value: Boolean) {
        properties.WATCH_INITIALIZATION = value
    }

    override fun getDisabledIcon(isMuted: Boolean): Icon {
        val master = DebuggerManagerEx.getInstanceEx(myProject).breakpointManager.findMasterBreakpoint(this)
        return when {
            isMuted && master == null -> AllIcons.Debugger.Db_muted_disabled_field_breakpoint
            isMuted && master != null -> AllIcons.Debugger.Db_muted_dep_field_breakpoint
            master != null -> AllIcons.Debugger.Db_dep_field_breakpoint
            else -> AllIcons.Debugger.Db_disabled_field_breakpoint
        }
    }

    override fun getSetIcon(isMuted: Boolean): Icon {
        return when {
            isMuted -> AllIcons.Debugger.Db_muted_field_breakpoint
            else -> AllIcons.Debugger.Db_field_breakpoint
        }
    }

    override fun getInvalidIcon(isMuted: Boolean): Icon {
        return when {
            isMuted -> AllIcons.Debugger.Db_muted_invalid_field_breakpoint
            else -> AllIcons.Debugger.Db_invalid_field_breakpoint
        }
    }

    override fun getVerifiedIcon(isMuted: Boolean): Icon {
        return when {
            isMuted -> AllIcons.Debugger.Db_muted_verified_field_breakpoint
            else -> AllIcons.Debugger.Db_verified_field_breakpoint
        }
    }

    override fun getVerifiedWarningsIcon(isMuted: Boolean): Icon {
        return when {
            isMuted -> AllIcons.Debugger.Db_muted_field_warning_breakpoint
            else -> AllIcons.Debugger.Db_field_warning_breakpoint
        }
    }

    override fun getCategory() = CATEGORY

    override fun getDisplayName(): String? {
        if (!isValid) {
            return DebuggerBundle.message("status.breakpoint.invalid")
        }
        val className = className
        return if (className != null && !className.isEmpty()) className + "." + getFieldName() else getFieldName()
    }

    private fun getFieldName(): String {
        val declaration = getField()
        return runReadAction { declaration?.name } ?: "unknown"
    }

    override fun getEvaluationElement(): PsiElement? {
        return getField()
    }

}