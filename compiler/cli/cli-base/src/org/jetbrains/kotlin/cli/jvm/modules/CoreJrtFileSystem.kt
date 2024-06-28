/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.io.URLUtil
import java.io.File
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.FileSystem
import java.nio.file.FileSystems

// There's JrtFileSystem in idea-full which we can't use in the compiler because it depends on NewVirtualFileSystem, absent in intellij-core
class CoreJrtFileSystem : DeprecatedVirtualFileSystem() {
    private val roots =
        ConcurrentFactoryMap.createMap<String, CoreJrtVirtualFile?> { jdkHomePath ->
            val fileSystem = globalJrtFsCache[jdkHomePath] ?: return@createMap null
            CoreJrtVirtualFile(this, jdkHomePath, fileSystem.getPath(""), parent = null)
        }

    override fun getProtocol(): String = StandardFileSystems.JRT_PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? {
        val (jdkHomePath, pathInImage) = splitPath(path)
        val root = roots[jdkHomePath] ?: return null

        if (pathInImage.isEmpty()) return root

        return root.findFileByRelativePath(pathInImage)
    }

    override fun refresh(asynchronous: Boolean) {}

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

    fun clearRoots() {
        roots.clear()
    }

    companion object {
        private fun loadJrtFsJar(jdkHome: File): File? =
            File(jdkHome, "lib/jrt-fs.jar").takeIf(File::exists)

        fun isModularJdk(jdkHome: File): Boolean =
            loadJrtFsJar(jdkHome) != null

        fun splitPath(path: String): Pair<String, String> {
            val separator = path.indexOf(URLUtil.JAR_SEPARATOR)
            if (separator < 0) {
                throw IllegalArgumentException("Path in CoreJrtFileSystem must contain a separator: $path")
            }
            val localPath = path.substring(0, separator)
            val pathInJar = path.substring(separator + URLUtil.JAR_SEPARATOR.length)
            return Pair(localPath, pathInJar)
        }

        private val globalJrtFsCache = ConcurrentFactoryMap.createMap<String, FileSystem?> { jdkHomePath ->
            val jdkHome = File(jdkHomePath)
            val jrtFsJar = loadJrtFsJar(jdkHome) ?: return@createMap null
            val rootUri = URI.create(StandardFileSystems.JRT_PROTOCOL + ":/")
            /*
              The ClassLoader, that was used to load JRT FS Provider actually lives as long as current thread due to ThreadLocal leak in jrt-fs,
              See https://bugs.openjdk.java.net/browse/JDK-8260621
              So that cache allows us to avoid creating too many classloaders for same JDK and reduce severity of that leak
            */
            if (isAtLeastJava9()) {
                // If the runtime JDK is set to 9+ it has JrtFileSystemProvider,
                // but to load proper jrt-fs (one that is pointed by jdkHome) we should provide "java.home" path
                FileSystems.newFileSystem(rootUri, mapOf("java.home" to jdkHome.absolutePath))
            } else {
                val classLoader = URLClassLoader(arrayOf(jrtFsJar.toURI().toURL()), null)
                // If the runtime JDK is set to <9, there are no JrtFileSystemProvider,
                // we should create classloader with jrt-fs.jar, and DO NOT NEED to pass "java.home" path,
                // as otherwise it will incur additional classloader creation
                FileSystems.newFileSystem(rootUri, emptyMap<String, Nothing>(), classLoader)
            }
        }
    }
}
