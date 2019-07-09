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
    buildString {
        append(subsystemName).append(" Internal error: ").appendln(message)

        if (location != null) {
            append("File being compiled at position: ").appendln(location)
        } else {
            append("Element is unknown")
        }

        if (cause != null) {
            append("The root cause ${cause::class.java.name} was thrown at: ")
            append(cause.stackTrace?.firstOrNull()?.toString() ?: "unknown")
        }
    }
}
