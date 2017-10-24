/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project

fun <T : Any> runInReadActionWithWriteActionPriorityWithPCE(f: () -> T): T {
    var r: T? = null
    if (!with(ApplicationManager.getApplication()) { isDispatchThread && isUnitTestMode }) {
        val complete = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority {
            r = f()
        }

        // There is a write action in progress or pending, so no point in counting the result
        if (!complete) throw ProcessCanceledException()
    }
    else {
        r = f()
    }

    return r!!
}

fun <T : Any> runInReadActionWithWriteActionPriority(f: () -> T): T? {
    var r: T? = null
    val complete = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority {
        r = f()
    }

    if (!complete) return null
    return r
}



fun <T : Any> Project.runSynchronouslyWithProgress(progressTitle: String, canBeCanceled: Boolean, action: () -> T): T? {
    var result: T? = null
    ProgressManager.getInstance().runProcessWithProgressSynchronously({ result = action() }, progressTitle, canBeCanceled, this)
    return result
}