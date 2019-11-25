/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.application.Application
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty

@set: org.jetbrains.annotations.TestOnly
var Application.isScriptChangesNotifierDisabled by NotNullableUserDataProperty(
    Key.create("SCRIPT_CHANGES_NOTIFIER_DISABLED"),
    false
)

internal val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.script")

internal fun debug(file: KtFile, message: () -> String) {
    debug(file.originalFile.virtualFile, message)
}

internal fun debug(file: VirtualFile? = null, message: () -> String) {
    if (LOG.isDebugEnabled) {
        LOG.debug("[KOTLIN SCRIPT] ${file?.let { file.path + " "} ?: ""}" + message())
    }
}