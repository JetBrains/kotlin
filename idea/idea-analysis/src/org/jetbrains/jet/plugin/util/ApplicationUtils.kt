/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util.application

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.ShutDownTracker
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.CommandProcessor

public fun warnTimeConsuming(logger: Logger) {
    val application = ApplicationManager.getApplication()!!
    if (application.isUnitTestMode() || application.isHeadlessEnvironment() || ShutDownTracker.isShutdownHookRunning()) {
        return
    }

    if (!application.isDispatchThread()) {
        return
    }

    logger.warn("This operation is time consuming and must not be called on EDT.")
    java.lang.Throwable().printStackTrace()
}

public fun runReadAction<T: Any>(action: () -> T?): T? {
    return ApplicationManager.getApplication()?.runReadAction<T>(action)
}

public fun runWriteAction<T: Any>(action: () -> T?): T? {
    return ApplicationManager.getApplication()?.runWriteAction<T>(action)
}

public fun Project.executeWriteCommand(name: String, command: () -> Unit) {
    CommandProcessor.getInstance().executeCommand(this, { runWriteAction(command) }, name, null)
}

public fun <T: Any> Project.executeWriteCommand(name: String, command: () -> T): T {
    var result: T? = null
    CommandProcessor.getInstance().executeCommand(this, { result = runWriteAction(command) }, name, null)
    return result!!
}