/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

fun showYesNoCancelDialog(key: String, project: Project, message: String, title: String, icon: Icon, default: Int?): Int {
    return if (!ApplicationManager.getApplication().isUnitTestMode) {
        Messages.showYesNoCancelDialog(project, message, title, icon)
    } else {
        callInTestMode(key, default)
    }
}

private val dialogResults = ConcurrentHashMap<String, Any>()

@TestOnly
fun setDialogsResult(key: String, result: Any) {
    dialogResults[key] = result
}

@TestOnly
fun clearDialogsResults() {
    dialogResults.clear()
}

private fun <T : Any?> callInTestMode(key: String, default: T?): T {
    val result = dialogResults[key]
    if (result != null) {
        dialogResults.remove(key)
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    if (default != null) {
        return default
    }

    throw IllegalStateException("Can't call '$key' dialog in test mode")
}

