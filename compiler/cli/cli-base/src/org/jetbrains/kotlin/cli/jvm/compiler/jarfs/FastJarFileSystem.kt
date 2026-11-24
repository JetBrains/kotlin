/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import com.intellij.openapi.util.Couple
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class FastJarFileSystem private constructor(unmapBuffer: MappedByteBuffer.() -> Unit) : DeprecatedVirtualFileSystem() {

    val archives = JarArchiveCache(unmapBuffer) { path, e ->
        // copying the behavior of ArchiveHandler (and therefore ZipHandler)
        // TODO: consider propagating to compiler error or warning, but take into account that both javac and K1 simply ignore invalid jars in such cases
        System.err.println("WARN: Error while reading zip file: $path: $e")
        e.printStackTrace(System.err)
    }

    private val myHandlers = ConcurrentHashMap<String, FastJarHandler>()

    override fun getProtocol(): String {
        return StandardFileSystems.JAR_PROTOCOL
    }

    override fun findFileByPath(path: String): VirtualFile? {
        val pair = splitPath(path)
        return handler(pair.first).findFileByPath(pair.second)
    }

    private fun handler(localPath: String): FastJarHandler {
        myHandlers[localPath]?.let { return it }
        val handler = FastJarHandler(this, localPath)
        return myHandlers.putIfAbsent(localPath, handler) ?: handler
    }

    override fun refresh(asynchronous: Boolean) {}
    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return findFileByPath(path)
    }

    fun clearHandlersCache() {
        myHandlers.clear()
        archives.clear()
    }

    fun cleanOpenFilesCache() {
        archives.closeOpenArchives()
    }

    companion object {
        fun splitPath(path: String): Couple<String> {
            val separator = path.indexOf("!/")
            require(separator >= 0) { "Path in JarFileSystem must contain a separator: $path" }
            val localPath = Paths.get(path.substring(0, separator)).toAbsolutePath().normalize().toString()
            val pathInJar = path.substring(separator + 2)
            return Couple.of(localPath, pathInJar)
        }

        fun createIfUnmappingPossible(): FastJarFileSystem? {
            val cleanerCallBack = prepareCleanerCallback() ?: return null
            return FastJarFileSystem(cleanerCallBack)
        }
    }
}


private val IS_PRIOR_9_JRE = System.getProperty("java.specification.version", "").startsWith("1.")

private fun prepareCleanerCallback(): ((ByteBuffer) -> Unit)? {
    return try {
        if (IS_PRIOR_9_JRE) {
            val cleaner = Class.forName("java.nio.DirectByteBuffer").getMethod("cleaner")
            cleaner.isAccessible = true

            val clean = Class.forName("sun.misc.Cleaner").getMethod("clean")
            clean.isAccessible = true

            { buffer: ByteBuffer -> cleaner.invoke(buffer)?.let { clean.invoke(it) } }
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

            val theUnsafe = theUnsafeField.get(null);

            { buffer: ByteBuffer -> clean.invoke(theUnsafe, buffer) }
        }
    } catch (ex: Exception) {
        null
    }
}
