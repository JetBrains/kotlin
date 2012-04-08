package org.jetbrains.jet.samples.vfs;

import kotlin.concurrent.*
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jetbrains.jet.samples.vfs.utils.*;
import java.util.concurrent.locks.Lock
import java.io.File
import java.util.List
import java.util.ArrayList
import java.util.HashMap
import java.util.Arrays
import java.util.Map

import kotlin.util.*
import java.util.TimerTask

/**
 * File system singleton. To work with virtual file system, read/write locks should be
 * acquired: use read() and write() higher-order functions.
 */
public object FileSystem {
    private val lock = ReentrantReadWriteLock()
    internal val watchedDirectories = ArrayList<VirtualFile>

    // FIXME VirtualFiles should be used as hashmap keys themselves,
    // but overriden hashCode() method fails in runtime with ClassCastException (KT-1134)
    /**
     * Mapping from virtual files to metainformation
     */
    internal val fileToInfo = HashMap<String, VirtualFileInfo>()
    private val listeners = ArrayList<VirtualFileListener>()

    /**
     * Returns corresponding virtual file for java.io.File.
     *
     * @param ioFile file
     */
    public fun getFileByIoFile(ioFile : File) : VirtualFile {
        FileSystem.assertCanRead()
        return PhysicalVirtualFile(ioFile.getAbsolutePath().sure().toSystemIndependentPath())
    }

    /**
     * Returns virtual file for path to it.
     *
     * @param path path to file
     */
    public fun getFileByPath(path : String) : VirtualFile {
        return getFileByIoFile(File(path))
    }

    /**
     * Runs function with read lock.
     */
    public inline fun read<T>(task : () -> T) : T {
        return lock.read<T>(task)
    }

    /**
     * Runs function with write lock.
     */
    public inline fun write<T>(task : () -> T) : T {
        return lock.write(task)
    }

    /**
     * Adds directory to list of watched directories. Watched directories are
     * periodically refreshed and corresponding events are sent to file system listeners.
     *
     * @param dir directory which should be watched
     */
    public fun addWatchedDirectory(dir : VirtualFile) {
        assertCanRead()
        if (dir.isDirectory) {
            watchedDirectories.add(dir)
            scanAndAddRecursivelyNoEvents(dir)

            RefreshQueue.scheduleRefresh()
        }
    }

    /* Scans file recursively and adds info about it to fileToInfo map */
    private fun scanAndAddRecursivelyNoEvents(file : VirtualFile) {
        require(FileSystem.fileToInfo[file.path] == null)

        val fileInfo = VirtualFileInfo(file)
        FileSystem.fileToInfo[file.path] = fileInfo
        fileInfo.children.forEach{ scanAndAddRecursivelyNoEvents(it) }
    }

    internal inline fun assertCanRead() {
        check(lock.getReadHoldCount() != 0 || lock.isWriteLockedByCurrentThread())
    }

    internal inline fun assertCanWrite() {
        check(lock.isWriteLockedByCurrentThread())
    }

    /**
     * Adds file system listener which should be notified about changing of file system.
     */
    public fun addVirtualFileListener(listener : VirtualFileListener) {
        listeners.add(listener)
    }

    /**
     * Adds file system listener which should be notified about changing of file system.
     */
    public fun addVirtualFileListener(listener : (VirtualFileEvent)->Unit) : VirtualFileListener {
        val vfl = SimpleVirtualFileListener(listener)
        addVirtualFileListener(vfl)
        return vfl
    }

    /**
     * Removes file system listener.
     */
    public fun removeVirtualFileListener(listener : VirtualFileListener) {
        listeners.remove(listener)
    }

    /* Notifies all listeners */
    internal fun notifyEventHappened(event : VirtualFileEvent) {
        for (listener in listeners) {
            listener.eventHappened(event)
        }
    }
}

private class VirtualFileInfo(file : VirtualFile) {
    /* Last modification time */
    var lastModified : Long = 0
    /* List of known children */
    val children : List<VirtualFile> = ArrayList<VirtualFile>;

    {
        children.addAll(file.children)
        lastModified = file.modificationTime
    }
}
