/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.openapi.application.ApplicationManager

fun getExceptionMessage(
    subsystemName: String,
    message: String,
    cause: Throwable?,
    location: String?
): String = ApplicationManager.getApplication().runReadAction<String> {
    val result = StringBuilder(subsystemName + " Internal error: ").append(message).append("\n")
    if (cause != null) {
        val causeMessage = cause.message
        result.append("Cause: ").append(causeMessage ?: cause.toString()).append("\n")
    }

    if (location != null) {
        result.append("File being compiled at position: ").append(location).append("\n")
    } else {
        result.append("Element is unknown")
    }

    if (cause != null) {
        result.append("The root cause was thrown at: ").append(where(cause))
    }

    result.toString()
}

private fun where(cause: Throwable): String {
    val stackTrace = cause.stackTrace
    if (stackTrace != null && stackTrace.size > 0) {
        return stackTrace[0].fileName + ":" + stackTrace[0].lineNumber
    }
    return "unknown"
}
