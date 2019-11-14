/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.codegen.inline.KOTLIN_STRATA_NAME
import org.jetbrains.kotlin.idea.debugger.isDexDebug
import org.jetbrains.kotlin.idea.debugger.safeSourceName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.util.containingNonLocalDeclaration

class KotlinLineBreakpoint(
    project: Project?,
    xBreakpoint: XBreakpoint<out XBreakpointProperties<*>>?
) : KotlinLineBreakpointBase(project, xBreakpoint) {
    override fun processClassPrepare(debugProcess: DebugProcess?, classType: ReferenceType?) {
        val sourcePosition = xBreakpoint?.sourcePosition

        if (classType != null && sourcePosition != null) {
            if (!hasTargetLine(classType, sourcePosition)) {
                return
            }
        }

        super.processClassPrepare(debugProcess, classType)
    }

    /**
     * Returns false if `classType` definitely does not contain a location for a given `sourcePosition`.
     */
    private fun hasTargetLine(classType: ReferenceType, sourcePosition: XSourcePosition): Boolean {
        val allLineLocations = DebuggerUtilsEx.allLineLocations(classType) ?: return true

        if (classType.virtualMachine().isDexDebug()) {
            return true
        }

        val fileName = sourcePosition.file.name
        val lineNumber = sourcePosition.line + 1

        for (location in allLineLocations) {
            val kotlinFileName = location.safeSourceName(KOTLIN_STRATA_NAME)
            val kotlinLineNumber = location.lineNumber(KOTLIN_STRATA_NAME)

            if (kotlinFileName != null) {
                if (kotlinFileName == fileName && kotlinLineNumber == lineNumber) {
                    return true
                }
            } else {
                if (location.safeSourceName() == fileName && location.lineNumber() == lineNumber) {
                    return true
                }
            }
        }

        return false
    }

    override fun getMethodName(): String? {
        val element = sourcePosition?.elementAt?.getNonStrictParentOfType<KtElement>()
        if (element is KtElement) {
            element.containingNonLocalDeclaration()?.name?.let { return it }
        }

        return super.getMethodName()
    }
}