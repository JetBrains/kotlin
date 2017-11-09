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
    }
    else {
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

private fun <T: Any?> callInTestMode(key: String, default: T?): T {
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

