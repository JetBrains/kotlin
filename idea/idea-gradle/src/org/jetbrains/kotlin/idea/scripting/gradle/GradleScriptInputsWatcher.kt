/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.PathUtil
import org.jetbrains.annotations.TestOnly

@State(
    name = "KotlinBuildScriptsModificationInfo",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class GradleScriptInputsWatcher(val project: Project) : PersistentStateComponent<GradleScriptInputsWatcher.Storage> {
    private var storage = Storage()

    fun startWatching() {
        project.messageBus.connect().subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    if (project.isDisposed) return

                    val files = getAffectedGradleProjectFiles(project)
                    for (event in events) {
                        val file = event.file ?: return
                        if (isInAffectedGradleProjectFiles(files, file)) {
                            storage.fileChanged(file, file.timeStamp)
                        }
                    }
                }
            })
    }

    fun areRelatedFilesUpToDate(file: VirtualFile, timeStamp: Long): Boolean {
        return storage.lastModifiedTimeStampExcept(file) < timeStamp
    }

    class Storage(
        private var lastModifiedTS: Long = 0,
        private var lastModifiedFiles: MutableSet<String> = hashSetOf(),
        private var previousModifiedTS: Long = 0,
        private var previousModifiedFiles: MutableSet<String> = hashSetOf()
    ) {
        fun lastModifiedTimeStampExcept(file: VirtualFile): Long {
            val fileId = getFileId(file)
            synchronized(this) {
                if (lastModifiedFiles.contains(fileId) && lastModifiedFiles.size == 1) {
                    return previousModifiedTS
                }
                return lastModifiedTS
            }

        }

        fun fileChanged(file: VirtualFile, ts: Long) {
            val fileId = getFileId(file)
            synchronized(this) {
                when {
                    ts > lastModifiedTS -> {
                        previousModifiedFiles = lastModifiedFiles
                        previousModifiedTS = lastModifiedTS

                        lastModifiedFiles = hashSetOf(fileId)
                        lastModifiedTS = ts
                    }
                    ts == lastModifiedTS -> {
                        lastModifiedFiles.add(fileId)
                    }
                    ts == previousModifiedTS -> {
                        previousModifiedFiles.add(fileId)
                    }
                    else -> {}
                }
            }
        }

        private fun getFileId(file: VirtualFile): String {
            val canonized = PathUtil.getCanonicalPath(file.path)
            return FileUtil.toSystemIndependentName(canonized)
        }
    }

    override fun getState(): Storage {
        return storage
    }

    override fun loadState(state: Storage) {
        this.storage = state
    }

    @TestOnly
    fun clearAndRefillState() {
        loadState(project.service<GradleScriptInputsWatcher>().state)
    }

    @TestOnly
    fun fileChanged(file: VirtualFile, ts: Long) {
        storage.fileChanged(file, ts)
    }
}