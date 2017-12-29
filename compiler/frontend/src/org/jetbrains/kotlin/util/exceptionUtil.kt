/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
            result.append("File being compiled and position: ").append(location).append("\n")
        }
        else {
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
