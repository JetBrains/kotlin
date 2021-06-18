package org.jetbrains.kotlin.idea.fir.low.level.api.test.base

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor

internal inline fun <T> runUndoTransparentWriteAction(crossinline runnable: () -> T): T {
    var result: T? = null
    CommandProcessor.getInstance().runUndoTransparentAction {
        ApplicationManager.getApplication().runWriteAction { result = runnable() }
    }
    @Suppress("UNCHECKED_CAST")
    return result as T
}