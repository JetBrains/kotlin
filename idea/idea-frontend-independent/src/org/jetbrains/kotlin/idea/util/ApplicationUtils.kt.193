/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util.application

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls

fun <T> runReadAction(action: () -> T): T {
    return ApplicationManager.getApplication().runReadAction<T>(action)
}

fun <T> runWriteAction(action: () -> T): T {
    return ApplicationManager.getApplication().runWriteAction<T>(action)
}

fun Project.executeWriteCommand(@Nls name: String, command: () -> Unit) {
    CommandProcessor.getInstance().executeCommand(this, { runWriteAction(command) }, name, null)
}

fun <T> Project.executeWriteCommand(@Nls name: String, groupId: Any? = null, command: () -> T): T {
    return executeCommand<T>(name, groupId) { runWriteAction(command) }
}

fun <T> Project.executeCommand(@Nls name: String, groupId: Any? = null, command: () -> T): T {
    @Suppress("UNCHECKED_CAST") var result: T = null as T
    CommandProcessor.getInstance().executeCommand(this, { result = command() }, name, groupId)
    @Suppress("USELESS_CAST")
    return result as T
}

fun <T> runWithCancellationCheck(block: () -> T): T = block()

inline fun executeOnPooledThread(crossinline action: () -> Unit) =
    ApplicationManager.getApplication().executeOnPooledThread { action() }

inline fun invokeLater(crossinline action: () -> Unit) =
    ApplicationManager.getApplication().invokeLater { action() }

inline fun isUnitTestMode(): Boolean = ApplicationManager.getApplication().isUnitTestMode

inline fun <reified T : Any> ComponentManager.getServiceSafe(): T =
    this.getService(T::class.java) ?: error("Unable to locate service ${T::class.java.name}")
