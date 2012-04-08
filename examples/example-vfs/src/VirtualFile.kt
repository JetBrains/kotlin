package org.jetbrains.jet.samples.vfs;

import org.jetbrains.jet.samples.vfs.utils.*;
import java.io.File
import java.io.InputStream
import java.io.FileInputStream
import java.util.ArrayList
import java.util.List

import kotlin.util.*

/**
 * Abstract virtual file.
 */
public abstract class VirtualFile(public val path : String) : Hashable {
    protected abstract val kind : String

    /**
     * Returns file size.
     */
    public abstract val size : Long
    /**
     * Returns the time that the virtual file was last modified (milliseconds since
     * the epoch (00:00:00 GMT, January 1, 1970).
     */
    public abstract val modificationTime : Long
    /**
     * Returns if virtual file exists.
     */
    public abstract val exists : Boolean
    /**
     * Returns if virtual file is directory
     */
    public abstract val isDirectory : Boolean
    /**
     * Returns list of virtual files which are children for this.
     */
    public abstract val children : List<VirtualFile>

    /**
     * Opens input stream for reading. After reading, stream should be closed.
     * Reading from stream should be performed with read lock acquired.
     */
    public abstract fun openInputStream() : InputStream

    public override fun equals(other : Any?) : Boolean {
        return other is VirtualFile && kind == other.kind && path == other.path
    }

    public override fun hashCode() : Int {
        // FIXME rewrite without casting when it will be possible (KT-1741)
        return (kind as java.lang.String).hashCode() * 31 + (path as java.lang.String).hashCode()
    }

    fun toString(): String {
        return "${kind}[path=$path]"
    }
}

/**
 * Type of virtual file which corresponds to real file in file system of OS.
 */
public class PhysicalVirtualFile(path : String) : VirtualFile(path) {
    override public val kind : String = "Physical"

    private val ioFile : File
        get() = File(this.path.toSystemDependentPath())

    override public val exists: Boolean
    get() {
        FileSystem.assertCanRead()
        return ioFile.exists()
    }

    override public val size: Long
    get() {
        FileSystem.assertCanRead()
        return ioFile.length()
    }

    override public val modificationTime: Long
    get() {
        FileSystem.assertCanRead()
        return ioFile.lastModified()
    }

    override public val isDirectory: Boolean
    get() {
        FileSystem.assertCanRead()
        return ioFile.isDirectory()
    }

    override public val children: List<VirtualFile>
    get() {
        FileSystem.assertCanRead()
        return (ioFile.listFiles() ?: array<File?>()).
                map{ FileSystem.getFileByIoFile(it.sure()) }?.toList()
    }

    override public fun openInputStream(): InputStream {
        FileSystem.assertCanRead()
        if (isDirectory) {
            throw IllegalArgumentException("Can't open directory for reading");
        }
        return CheckedInputStream(FileInputStream(ioFile))
    }
}

private val OS_SEPARATOR = java.io.File.separator.sure()
private val VFS_SEPARATOR = "/"

private fun String.toSystemDependentPath() : String {
    return this.replaceAll(VFS_SEPARATOR, OS_SEPARATOR)
}

private fun String.toSystemIndependentPath() : String {
    return this.replaceAll(OS_SEPARATOR, VFS_SEPARATOR)
}

/**
 * InputStream wrapper which checks that file system read lock is acquired on each operation.
 */
private class CheckedInputStream(private val wrapped : InputStream) : InputStream() {
    override public fun read(): Int {
        FileSystem.assertCanRead()
        return wrapped.read()
    }

    override public fun read(b: ByteArray?, off: Int, len: Int) : Int {
        FileSystem.assertCanRead()
        return wrapped.read(b, off, len)
    }

    override public fun markSupported(): Boolean {
        FileSystem.assertCanRead()
        return wrapped.markSupported()
    }

    override public fun skip(n: Long): Long {
        FileSystem.assertCanRead()
        return wrapped.skip(n)
    }

    override public fun close() {
        FileSystem.assertCanRead()
        return wrapped.close()
    }

    override public fun mark(readlimit: Int) {
        FileSystem.assertCanRead()
        return wrapped.mark(readlimit)
    }

    override public fun read(b: ByteArray?): Int {
        FileSystem.assertCanRead()
        return wrapped.read(b)
    }

    override public fun reset() {
        FileSystem.assertCanRead()
        return wrapped.reset()
    }

    override public fun available(): Int {
        FileSystem.assertCanRead()
        return wrapped.available()
    }
}