/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly

@State(
    name = "KotlinBuildScriptsModificationInfo",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class GradleScriptInputsWatcher(val project: Project) : PersistentStateComponent<GradleScriptInputsWatcher.Storage> {
    private var storage = Storage()

    fun startWatching() {
        addVfsListener(this)
    }

    fun areRelatedFilesUpToDate(file: VirtualFile, timeStamp: Long): Boolean {
        return storage.lastModifiedTimeStampExcept(file.path) < timeStamp
    }

    class Storage {
        private val lastModifiedFiles = LastModifiedFiles()

        fun lastModifiedTimeStampExcept(filePath: String): Long {
            return lastModifiedFiles.lastModifiedTimeStampExcept(filePath)
        }

        fun fileChanged(filePath: String, ts: Long) {
            lastModifiedFiles.fileChanged(ts, filePath)
        }
    }

    override fun getState(): Storage {
        return storage
    }

    override fun loadState(state: Storage) {
        this.storage = state
    }

    fun fileChanged(filePath: String, ts: Long) {
        storage.fileChanged(filePath, ts)
    }

    fun clearState() {
        storage = Storage()
    }

    @TestOnly
    fun clearAndRefillState() {
        loadState(project.service<GradleScriptInputsWatcher>().state)
    }
}