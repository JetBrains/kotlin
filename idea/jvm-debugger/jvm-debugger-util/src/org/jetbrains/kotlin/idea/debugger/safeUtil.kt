/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.evaluation.AbsentInformationEvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.jdi.StackFrameProxy
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.LocalVariableProxyImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.sun.jdi.*

fun StackFrameProxyImpl.safeVisibleVariables(): List<LocalVariableProxyImpl> {
    return wrapAbsentInformationException { visibleVariables() } ?: emptyList()
}

fun StackFrameProxyImpl.safeVisibleVariableByName(name: String): LocalVariableProxyImpl? {
    return wrapAbsentInformationException { visibleVariableByName(name) }
}

fun Method.safeAllLineLocations(): List<Location> {
    return DebuggerUtilsEx.allLineLocations(this) ?: emptyList()
}

fun ReferenceType.safeAllLineLocations(): List<Location> {
    return DebuggerUtilsEx.allLineLocations(this) ?: emptyList()
}

fun ReferenceType.safeSourceName(): String? {
    return wrapAbsentInformationException { sourceName() }
}

fun ReferenceType.safeFields(): List<Field> {
    return try {
        fields()
    } catch (e: ClassNotPreparedException) {
        emptyList()
    }
}

fun Method.safeLocationsOfLine(line: Int): List<Location> {
    return wrapAbsentInformationException { locationsOfLine(line) } ?: emptyList()
}

fun Method.safeVariables(): List<LocalVariable>? {
    return wrapAbsentInformationException { variables() }
}

fun Method.safeArguments(): List<LocalVariable>? {
    return wrapAbsentInformationException { arguments() }
}

fun StackFrameProxy.safeLocation(): Location? {
    return try {
        this.location()
    } catch (e: EvaluateException) {
        null
    }
}

fun Location.safeSourceName(): String? {
    return wrapAbsentInformationException { this.sourceName() }
}

fun Location.safeSourceName(stratum: String): String? {
    return wrapAbsentInformationException { this.sourceName(stratum) }
}

fun Location.safeLineNumber(): Int {
    return DebuggerUtilsEx.getLineNumber(this, false)
}

fun Location.safeSourceLineNumber(): Int {
    return DebuggerUtilsEx.getLineNumber(this, true)
}

fun Location.safeMethod(): Method? {
    return DebuggerUtilsEx.getMethod(this)
}

fun LocalVariableProxyImpl.safeType(): Type? {
    return wrapClassNotLoadedException { type }
}

fun Field.safeType(): Type? {
    return wrapClassNotLoadedException { type() }
}

private inline fun <T> wrapAbsentInformationException(block: () -> T): T? {
    return try {
        block()
    } catch (e: AbsentInformationException) {
        null
    } catch (e: AbsentInformationEvaluateException) {
        null
    } catch (e: InternalException) {
        null
    }
}

private inline fun <T> wrapClassNotLoadedException(block: () -> T): T? {
    return try {
        block()
    } catch (e: ClassNotLoadedException) {
        null
    }
}