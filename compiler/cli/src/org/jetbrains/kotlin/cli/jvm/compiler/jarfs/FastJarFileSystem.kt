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

class FastJarFileSystem : DeprecatedVirtualFileSystem() {
    private val myHandlers: MutableMap<String, FastJarHandler> =
        ConcurrentFactoryMap.createMap { key: String -> FastJarHandler(this@FastJarFileSystem, key) }

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
