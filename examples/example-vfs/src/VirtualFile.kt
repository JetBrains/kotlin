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
public abstract class VirtualFile(public val path : String) {
    // FIXME this method should be replaced with val (KT-1168, KT-1170)
    protected abstract fun kind() : String

    // FIXME these abstract methods should be replaced with vals (KT-1165)
    /**
     * Returns file size.
     */
    public abstract fun size() : Long
    /**
     * Returns the time that the virtual file was last modified (milliseconds since
     * the epoch (00:00:00 GMT, January 1, 1970).
     */
    public abstract fun modificationTime() : Long
    /**
     * Returns if virtual file exists.
     */
    public abstract fun exists() : Boolean
    /**
     * Returns if virtual file is directory
     */
    public abstract fun isDirectory() : Boolean
    /**
     * Returns list of virtual files which are children for this.
     */
    public abstract fun children() : List<VirtualFile>

    /**
     * Opens input stream for reading. After reading, stream should be closed.
     * Reading from stream should be performed with read lock acquired.
     */
    public abstract fun openInputStream() : InputStream

    fun equals(other : Any?) : Boolean {
        return other is VirtualFile && kind() == other.kind() && path == other.path
    }

    fun hashCode() : Int {
        // FIXME rewrite without casting when it will be possible
        return (kind() as java.lang.String).hashCode() * 31 + (path as java.lang.String).hashCode()
    }

    fun toString(): String {
        return "${kind()}[path=$path]"
    }
}

/**
 * Type of virtual file which corresponds to real file in file system of OS.
 */
public class PhysicalVirtualFile(path : String) : VirtualFile(path) {
    override fun kind() : String = "Physical"

    private val ioFile : File
        get() = File(this.path.toSystemDependentPath())

    override fun exists(): Boolean {
        FileSystem.assertCanRead()
        return ioFile.exists()
    }

    override fun size(): Long {
        FileSystem.assertCanRead()
        return ioFile.length()
    }

    override fun modificationTime(): Long {
        FileSystem.assertCanRead()
        return ioFile.lastModified()
    }

    override fun isDirectory(): Boolean {
        FileSystem.assertCanRead()
        return ioFile.isDirectory()
    }

    override fun children(): List<VirtualFile> {
        FileSystem.assertCanRead()
        return (ioFile.listFiles() ?: Array<File>(0)).
                map{ FileSystem.getFileByIoFile(it.sure()) }?.toList()
    }

    override fun openInputStream(): InputStream {
        FileSystem.assertCanRead()
        if (isDirectory()) {
            throw IllegalArgumentException("Can't open directory for reading");
        }
        return CheckedInputStream(FileInputStream(ioFile))
    }
}

private fun String.toSystemDependentPath() : String {
    // FIXME constants should be extracted (NPE in compiler, KT-1111)
    return this.replaceAll("/", java.io.File.separator.sure())
}

private fun String.toSystemIndependentPath() : String {
    // FIXME constants should be extracted (NPE in compiler, KT-1111)
    return this.replaceAll(java.io.File.separator.sure(), "/")
}

/**
 * InputStream wrapper which checks that file system read lock is acquired on each operation.
 */
private class CheckedInputStream(private val wrapped : InputStream) : InputStream() {
    override fun read(): Int {
        FileSystem.assertCanRead()
        return wrapped.read()
    }

    override fun read(b: ByteArray?, off: Int, len: Int) : Int {
        FileSystem.assertCanRead()
        return wrapped.read(b, off, len)
    }

    override fun markSupported(): Boolean {
        FileSystem.assertCanRead()
        return wrapped.markSupported()
    }

    override fun skip(n: Long): Long {
        FileSystem.assertCanRead()
        return wrapped.skip(n)
    }

    override fun close() {
        FileSystem.assertCanRead()
        return wrapped.close()
    }

    override fun mark(readlimit: Int) {
        FileSystem.assertCanRead()
        return wrapped.mark(readlimit)
    }

    override fun read(b: ByteArray?): Int {
        FileSystem.assertCanRead()
        return wrapped.read(b)
    }

    override fun reset() {
        FileSystem.assertCanRead()
        return wrapped.reset()
    }

    override fun available(): Int {
        FileSystem.assertCanRead()
        return wrapped.available()
    }
}