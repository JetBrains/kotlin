/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.externalSystem.autoimport.AsyncFileChangeListenerBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

fun addVfsListener(watcher: GradleScriptInputsWatcher) {
    VirtualFileManager.getInstance().addAsyncFileListener(
        object : AsyncFileChangeListenerBase() {
            override fun isRelevant(path: String): Boolean {
                return isInAffectedGradleProjectFiles(watcher.project, path)
            }

            override fun updateFile(file: VirtualFile, event: VFileEvent) {
                watcher.fileChanged(event.path, file.timeStamp)
            }

            // do nothing
            override fun apply() {}
            override fun init() {}

        },
        watcher.project
    )
}