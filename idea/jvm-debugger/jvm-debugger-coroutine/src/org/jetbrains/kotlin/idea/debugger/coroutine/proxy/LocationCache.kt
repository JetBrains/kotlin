/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.jdi.ClassesByNameProvider
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.util.containers.ContainerUtil
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext
import org.jetbrains.kotlin.utils.checkWithAttachment

class LocationCache(val context: DefaultExecutionContext) {
    private val classesByName = ClassesByNameProvider.createCache(context.vm.allClasses())

    fun createLocation(stackTraceElement: StackTraceElement): Location = createLocation(
        ContainerUtil.getFirstItem(classesByName[stackTraceElement.className]),
        stackTraceElement.methodName,
        stackTraceElement.lineNumber
    )

    fun createLocation(
        type: ReferenceType?,
        methodName: String,
        line: Int
    ): Location {
        if (type != null && line >= 0) {
            try {
                val location = type.locationsOfLine(DebugProcess.JAVA_STRATUM, null, line).stream()
                    .filter { l: Location -> l.method().name() == methodName }
                    .findFirst().orElse(null)
                if (location != null) {
                    return location
                }
            } catch (ignored: AbsentInformationException) {
            }
        }
        checkWithAttachment(type != null, {
            "Bad type: $type"
        }) {
            it.withAttachment("type", type)
            it.withAttachment("methodName", methodName)
            it.withAttachment("line", line)
        }
        return GeneratedLocation(context.debugProcess, type, methodName, line)
    }
}