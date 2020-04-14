/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.util

import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ApplicationThreadExecutor
import org.jetbrains.kotlin.idea.debugger.safeLineNumber
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod

fun getPosition(stackTraceElement: StackTraceElement, project: Project): XSourcePosition? {
    val psiFacade = JavaPsiFacade.getInstance(project)

    val psiClass = ApplicationThreadExecutor().readAction {
        @Suppress("DEPRECATION")
        psiFacade.findClass(
            stackTraceElement.className.substringBefore("$"), // find outer class, for which psi exists TODO
            GlobalSearchScope.everythingScope(project)
        )
    }

    val classFile = psiClass?.containingFile?.virtualFile
    // to convert to 0-based line number or '-1' to do not move
    val lineNumber = if (stackTraceElement.lineNumber > 0) stackTraceElement.lineNumber - 1 else return null
    return XDebuggerUtil.getInstance().createPosition(classFile, lineNumber)
}

fun Location.format(): String {
    val method = safeMethod()
    return "${method?.name() ?: "noname"}:${safeLineNumber()}, ${method?.declaringType()?.name() ?: "empty"}"
}

fun JavaStackFrame.format(): String {
    val location = descriptor.location
    return location?.let { it.format() } ?: "emptyLocation"
}

fun StackFrameItem.format(): String {
    val method = this.method()
    val type = this.path()
    val lineNumber = this.line()
    return "$method:$lineNumber, $type"
}

fun StackFrameProxyImpl.format(): String {
    return safeLocation()?.format() ?: "emptyLocation"
}

fun isInUnitTest() = ApplicationManager.getApplication().isUnitTestMode

fun coroutineDebuggerTraceEnabled() = Registry.`is`("kotlin.debugger.coroutines.trace") || isInUnitTest()
