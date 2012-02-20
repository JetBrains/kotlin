package org.jetbrains.jet.samples.vfs;

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.List
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.LinkedBlockingQueue
import java.util.ArrayList

import std.util.*
import std.concurrent.*
import java.util.Timer
import java.util.TimerTask

import org.jetbrains.jet.samples.vfs.utils.*
import org.jetbrains.jet.samples.vfs.utils.listDifference

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

        fixedRateTimer(daemon=true, name="refresher timer", period=5000.long) {
            scheduleRefresh(FileSystem.watchedDirectories)
        }
    }

    private fun takeAndRefreshFiles() {
        refreshFiles(taskQueue.take())
    }

    /* Acquires write lock and refreshes file system */
    private fun refreshFiles(files : List<VirtualFile>) {
        FileSystem.write {
            files.foreach{ refreshFile(it) }
        }
    }

    /* Checks for changes in virtual file recursively, notifying listeners. */
    private fun refreshFile(file : VirtualFile) {
        FileSystem.assertCanWrite()
        if (file.isDirectory()) {
            val fileToInfo = FileSystem.fileToInfo
            val fileInfo = fileToInfo[file.path]
            val oldChildren = fileInfo.children
            val newChildren = file.children()


            val addedChildren = listDifference(newChildren, oldChildren)
            val deletedChildren = listDifference(oldChildren, newChildren)
            val commonChildren = listIntersection(oldChildren, newChildren)

            addedChildren.foreach{ addRecursively(it) }
            deletedChildren.foreach{ deleteRecursively(it) }

            fileInfo.children.clear()
            fileInfo.children.addAll(newChildren)

            commonChildren.foreach{ refreshFile(it) }
        } else {
            val fileInfo = FileSystem.fileToInfo[file.path]
            assert(fileInfo != null)

            val newModificationTime = file.modificationTime()
            if (fileInfo.lastModified != newModificationTime) {
                fileInfo.lastModified = newModificationTime

                FileSystem.notifyEventHappened(VirtualFileChangedEvent(file))
            }
        }
    }

    /* Adds file to file system recursively, notifying listeners */
    private fun addRecursively(file : VirtualFile) {
        assert(FileSystem.fileToInfo[file] == null)

        val fileInfo = VirtualFileInfo(file)

        FileSystem.fileToInfo[file.path] = fileInfo
        FileSystem.notifyEventHappened(VirtualFileCreateEvent(file))

        fileInfo.children.foreach{ addRecursively(it) }
    }

    /* Deletes file from file system recursively, notifying listeners */
    private fun deleteRecursively(file : VirtualFile) {
        val fileInfoMaybe : VirtualFileInfo? = FileSystem.fileToInfo[file.path]
        val fileInfo = fileInfoMaybe.sure()
        fileInfo.children.foreach{ deleteRecursively(it) }
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
        files.foreach{ filesList.add(it) }
        taskQueue.put(filesList)

        // taskQueue.put(ArrayList<VirtualFile>(files.map{ it }))
        // taskQueue.put(files.map{ it }.toList())
    }
}
