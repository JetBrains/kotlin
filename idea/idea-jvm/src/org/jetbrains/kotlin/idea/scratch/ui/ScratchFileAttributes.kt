/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.ui

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.util.cachedFileAttribute
import org.jetbrains.kotlin.idea.scratch.ScratchFileOptions

var VirtualFile.scratchFileOptions: ScratchFileOptions? by cachedFileAttribute(
    name = "kotlin-scratch-file-options",
    version = 1,
    read = { ScratchFileOptions(readBoolean(), readBoolean(), readBoolean()) },
    write = {
        writeBoolean(it.isRepl)
        writeBoolean(it.isMakeBeforeRun)
        writeBoolean(it.isInteractiveMode)
    }
)