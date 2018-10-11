/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileTypeEvent
import com.intellij.openapi.fileTypes.FileTypeListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent

class LibraryModificationTracker(project: Project) : SimpleModificationTracker() {
    companion object {
        @JvmStatic
        fun getInstance(project: Project) = ServiceManager.getService(project, LibraryModificationTracker::class.java)!!
    }

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {
            override fun after(events: List<VFileEvent>) {
                events.filter(::isRelevantEvent).let { createEvents ->
                    if (createEvents.isNotEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            if (!project.isDisposed) {
                                processBulk(createEvents) {
                                    projectFileIndex.isInLibraryClasses(it) || isLibraryArchiveRoot(it)
                                }
                            }
                        }
                    }
                }
            }

            override fun before(events: List<VFileEvent>) {
                processBulk(events) {
                    projectFileIndex.isInLibraryClasses(it)
                }
            }
        })

        connection.subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            override fun enteredDumbMode() {
                incModificationCount()
            }

            override fun exitDumbMode() {
                incModificationCount()
            }
        })

        connection.subscribe(FileTypeManager.TOPIC, object : FileTypeListener {
            override fun beforeFileTypesChanged(event: FileTypeEvent) {
                incModificationCount()
            }

            override fun fileTypesChanged(event: FileTypeEvent) {
                incModificationCount()
            }
        })
    }

    private val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    private inline fun processBulk(events: List<VFileEvent>, check: (VirtualFile) -> Boolean) {
        events.forEach { event ->
            if (event.isValid) {
                val file = event.file
                if (file != null && check(file)) {
                    incModificationCount()
                    return
                }
            }
        }
    }

    // if library points to a jar, the jar does not pass isInLibraryClasses check, so we have to perform additional check for this case
    private fun isLibraryArchiveRoot(virtualFile: VirtualFile): Boolean {
        if (virtualFile.fileType != ArchiveFileType.INSTANCE) return false

        val archiveRoot = JarFileSystem.getInstance().getRootByLocal(virtualFile) ?: return false
        return projectFileIndex.isInLibraryClasses(archiveRoot)
    }
}

private fun isRelevantEvent(vFileEvent: VFileEvent) =
    vFileEvent is VFileCreateEvent || vFileEvent is VFileMoveEvent || vFileEvent is VFileCopyEvent
