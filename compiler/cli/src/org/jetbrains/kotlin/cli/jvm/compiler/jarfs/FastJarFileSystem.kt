/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.intellij.openapi.util.Couple
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.io.FileAccessorCache
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

private typealias RandomAccessFileAndBuffer = Pair<RandomAccessFile, MappedByteBuffer>

class FastJarFileSystem : DeprecatedVirtualFileSystem() {
    private val myHandlers: MutableMap<String, FastJarHandler> =
        ConcurrentFactoryMap.createMap { key: String -> FastJarHandler(this@FastJarFileSystem, key) }

    internal val cachedOpenFileHandles: FileAccessorCache<File, RandomAccessFileAndBuffer> =
        object : FileAccessorCache<File, RandomAccessFileAndBuffer>(20, 10) {
            @Throws(IOException::class)
            override fun createAccessor(file: File): RandomAccessFileAndBuffer {
                val randomAccessFile = RandomAccessFile(file, "r")
                return Pair(randomAccessFile, randomAccessFile.channel.map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length()))
            }

            @Throws(IOException::class)
            override fun disposeAccessor(fileAccessor: RandomAccessFileAndBuffer) {
                fileAccessor.first.close()
                fileAccessor.second.unmapBuffer()
            }

            override fun isEqual(val1: File, val2: File): Boolean {
                return val1 == val2 // reference equality to handle different jars for different ZipHandlers on the same path
            }
        }

    override fun getProtocol(): String {
        return StandardFileSystems.JAR_PROTOCOL
    }

    override fun findFileByPath(path: String): VirtualFile? {
        val pair = splitPath(path)
        return myHandlers[pair.first]!!.findFileByPath(pair.second)
    }

    override fun refresh(asynchronous: Boolean) {}
    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return findFileByPath(path)
    }

    fun clearHandlersCache() {
        myHandlers.clear()
        cachedOpenFileHandles.clear()
    }

    companion object {
        fun splitPath(path: String): Couple<String> {
            val separator = path.indexOf("!/")
            require(separator >= 0) { "Path in JarFileSystem must contain a separator: $path" }
            val localPath = path.substring(0, separator)
            val pathInJar = path.substring(separator + 2)
            return Couple.of(localPath, pathInJar)
        }
    }
}


private val IS_PRIOR_9_JRE = System.getProperty("java.specification.version", "").startsWith("1.")

internal fun ByteBuffer.unmapBuffer() {
    if (!isDirect) return

    try {
        if (IS_PRIOR_9_JRE) {
            val cleaner = this::class.java.getMethod("cleaner")

            cleaner.isAccessible = true

            val clean = Class.forName("sun.misc.Cleaner").getMethod("clean")
            clean.isAccessible = true

            clean.invoke(cleaner.invoke(this))
        } else {
            val unsafeClass = try {
                Class.forName("sun.misc.Unsafe")
            } catch (ex: Exception) {
                // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
                // but that method should be added if sun.misc.Unsafe is removed.
                Class.forName("jdk.internal.misc.Unsafe")
            }

            val clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer::class.java)
            clean.isAccessible = true

            val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe")
            theUnsafeField.isAccessible = true

            val theUnsafe = theUnsafeField.get(null)

            clean.invoke(theUnsafe, this)
        }
    } catch (ex: Exception) {
    }
}
