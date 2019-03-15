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
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

// There's JrtFileSystem in idea-full which we can't use in the compiler because it depends on NewVirtualFileSystem, absent in intellij-core
class CoreJrtFileSystem : DeprecatedVirtualFileSystem() {
    private val handlers =
        ConcurrentFactoryMap.createMap<String, CoreJrtHandler?> { jdkHomePath ->
            val jdkHome = File(jdkHomePath)
            val rootUri = URI.create(StandardFileSystems.JRT_PROTOCOL + ":/")
            val jrtFsJar = loadJrtFsJar(jdkHome) ?: return@createMap null
            val fileSystem =
                if (isAtLeastJava9()) {
                    FileSystems.newFileSystem(rootUri, mapOf("java.home" to jdkHome.absolutePath))
                } else {
                    val classLoader = URLClassLoader(arrayOf(jrtFsJar.toURI().toURL()), null)
                    FileSystems.newFileSystem(rootUri, emptyMap<String, Nothing>(), classLoader)
                }
            CoreJrtHandler(this, jdkHomePath, fileSystem.getPath(""))
        }

    internal class CoreJrtHandler(
        val virtualFileSystem: CoreJrtFileSystem,
        val jdkHomePath: String,
        private val root: Path
    ) {
        fun findFile(fileName: String): VirtualFile? {
            val path = root.resolve(fileName)
            return if (Files.exists(path)) CoreJrtVirtualFile(this, path) else null
        }
    }

    override fun getProtocol(): String = StandardFileSystems.JRT_PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? {
        val (jdkHomePath, pathInImage) = splitPath(path)
        return handlers[jdkHomePath]?.findFile(pathInImage)
    }

    private fun splitPath(path: String): Pair<String, String> {
        val separator = path.indexOf(URLUtil.JAR_SEPARATOR)
        if (separator < 0) {
            throw IllegalArgumentException("Path in CoreJrtFileSystem must contain a separator: $path")
        }
        val localPath = path.substring(0, separator)
        val pathInJar = path.substring(separator + 2)
        return Pair(localPath, pathInJar)
    }

    override fun refresh(asynchronous: Boolean) {}

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

    companion object {
        private fun loadJrtFsJar(jdkHome: File): File? =
            File(jdkHome, "lib/jrt-fs.jar").takeIf(File::exists)

        fun isModularJdk(jdkHome: File): Boolean =
            loadJrtFsJar(jdkHome) != null
    }
}
