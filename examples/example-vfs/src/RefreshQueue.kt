package org.jetbrains.jet.samples.vfs;

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.List
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.LinkedBlockingQueue
import java.util.ArrayList

import kotlin.util.*
import kotlin.concurrent.*
import java.util.Timer
import java.util.TimerTask

import org.jetbrains.jet.samples.vfs.utils.*

/**
 * Singleton which creates thread for periodically checking if there are changes in
 * file system and notifying file system listeners.
 */
internal object RefreshQueue {
    private val taskQueue = LinkedBlockingQueue<List<VirtualFile>>();

    {
        thread(daemon=true, name="refresher thread") {
            while (!currentThread.isInterrupted()) {
                try {
                    takeAndRefreshFiles()
                } catch (e : InterruptedException) {
                }
            }
        }

        fixedRateTimer(daemon=true, name="refresher timer", period=5000.toLong()) {
            scheduleRefresh(FileSystem.watchedDirectories)
        }
    }

    private fun takeAndRefreshFiles() {
        refreshFiles(taskQueue.take()!!)
    }

    /* Acquires write lock and refreshes file system */
    private fun refreshFiles(files : List<VirtualFile>) {
        FileSystem.write {
            files.forEach{ refreshFile(it) }
        }
    }

    /* Checks for changes in virtual file recursively, notifying listeners. */
    private fun refreshFile(file : VirtualFile) {
        FileSystem.assertCanWrite()
        val fileInfo = FileSystem.fileToInfo[file.path]
        check(fileInfo != null)
        if (fileInfo == null) {
            return
        }
        if (file.isDirectory) {
            val oldChildren = fileInfo.children
            val newChildren = file.children


            val addedChildren = listDifference(newChildren, oldChildren)
            val deletedChildren = listDifference(oldChildren, newChildren)
            val commonChildren = listIntersection(oldChildren, newChildren)

            addedChildren.forEach{ addRecursively(it) }
            deletedChildren.forEach{ deleteRecursively(it) }

            fileInfo.children.clear()
            fileInfo.children.addAll(newChildren)

            commonChildren.forEach{ refreshFile(it) }
        } else {
            val newModificationTime = file.modificationTime
            if (fileInfo.lastModified != newModificationTime) {
                fileInfo.lastModified = newModificationTime

                FileSystem.notifyEventHappened(VirtualFileChangedEvent(file))
            }
        }
    }

    /* Adds file to file system recursively, notifying listeners */
    private fun addRecursively(file : VirtualFile) {
        require(FileSystem.fileToInfo[file] == null)

        val fileInfo = VirtualFileInfo(file)

        FileSystem.fileToInfo[file] = fileInfo
        FileSystem.notifyEventHappened(VirtualFileCreateEvent(file))

        fileInfo.children.forEach{ addRecursively(it) }
    }

    /* Deletes file from file system recursively, notifying listeners */
    private fun deleteRecursively(file : VirtualFile) {
        val fileInfoMaybe : VirtualFileInfo? = FileSystem.fileToInfo[file.path]
        val fileInfo = fileInfoMaybe.sure()
        fileInfo.children.forEach{ deleteRecursively(it) }
        FileSystem.notifyEventHappened(VirtualFileDeletedEvent(file))
        FileSystem.fileToInfo.remove(file)
    }

    /**
     * Schedules refresh for given list of files.
     */
    public fun scheduleRefresh(files : List<VirtualFile>) {
        taskQueue.put(files)
    }

    /**
     * Schedules refresh for given list of files.
     */
    public fun scheduleRefresh(vararg files : VirtualFile) {
        // FIXME This could be written more concise, using map() & toList() (KT-1164 & KT-1172)
        val filesList = ArrayList<VirtualFile>()
        files.forEach{ filesList.add(it) }
        taskQueue.put(filesList)

        // taskQueue.put(ArrayList<VirtualFile>(files.map{ it }))
        // taskQueue.put(files.map{ it }.toList())
    }
}
