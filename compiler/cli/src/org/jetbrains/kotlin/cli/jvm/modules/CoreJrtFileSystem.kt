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

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

// There's JrtFileSystem in idea-full which we can't use in the compiler because it depends on NewVirtualFileSystem, absent in intellij-core
class CoreJrtFileSystem(private val fileSystem: FileSystem) : DeprecatedVirtualFileSystem() {
    override fun getProtocol(): String = StandardFileSystems.JRT_PROTOCOL

    private fun findFileByPath(path: Path): VirtualFile? =
            if (Files.exists(path)) CoreLocalPathVirtualFile(this, path) else null

    override fun findFileByPath(path: String): VirtualFile? =
            findFileByPath(fileSystem.getPath(path))

    override fun refresh(asynchronous: Boolean) {}

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

    companion object {
        fun create(jdkHome: File): CoreJrtFileSystem? {
            val rootUri = URI.create(StandardFileSystems.JRT_PROTOCOL + ":/")
            val fileSystem =
                    if (SystemInfo.isJavaVersionAtLeast("9")) {
                        FileSystems.newFileSystem(rootUri, mapOf("java.home" to jdkHome.absolutePath))
                    }
                    else {
                        val jrtFsJar = File(jdkHome, "lib/jrt-fs.jar")
                        if (!jrtFsJar.exists()) return null

                        val classLoader = URLClassLoader(arrayOf(jrtFsJar.toURI().toURL()), null)
                        FileSystems.newFileSystem(rootUri, emptyMap<String, Nothing>(), classLoader)
                    }
            return CoreJrtFileSystem(fileSystem)
        }
    }
}
