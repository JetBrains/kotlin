/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.js

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.openapi.vfs.newvfs.VfsImplUtil
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import kotlin.platform.platformStatic

public class KotlinJavaScriptMetaFileSystem : ArchiveFileSystem() {
    companion object {
        platformStatic
        public fun getInstance(): KotlinJavaScriptMetaFileSystem = VirtualFileManager.getInstance().getFileSystem(KotlinJavascriptMetadataUtils.VFS_PROTOCOL) as KotlinJavaScriptMetaFileSystem
    }

    private val ARCHIVE_SUFFIX = ".kjsm_archive"

    override fun getProtocol(): String = KotlinJavascriptMetadataUtils.VFS_PROTOCOL

    override fun extractRootPath(path: String): String {
        val jarSeparatorIndex = path.indexOf(JarFileSystem.JAR_SEPARATOR)
        assert(jarSeparatorIndex >= 0) { "Path passed to KotlinJavascriptMetaFileSystem must have separator '!/': " + path }
        return path.substring(0, jarSeparatorIndex + JarFileSystem.JAR_SEPARATOR.length())
    }

    override fun getHandler(entryFile: VirtualFile): KotlinJavaScriptHandler {
        val pathToRoot = extractLocalPath(this.extractRootPath(entryFile.getPath()))
        return VfsImplUtil.getHandler<KotlinJavaScriptHandler>(this, pathToRoot + ARCHIVE_SUFFIX) {
            KotlinJavaScriptHandler(it.substringBeforeLast(ARCHIVE_SUFFIX))
        }
    }

    override fun extractLocalPath(rootPath: String): String = StringUtil.trimEnd(rootPath, JarFileSystem.JAR_SEPARATOR)

    override fun composeRootPath(localPath: String): String = localPath + JarFileSystem.JAR_SEPARATOR

    override fun findFileByPath(path: String): VirtualFile? = VfsImplUtil.findFileByPath(this, path)

    override fun findFileByPathIfCached(path: String): VirtualFile? = VfsImplUtil.findFileByPathIfCached(this, path)

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = VfsImplUtil.refreshAndFindFileByPath(this, path)

    override fun refresh(asynchronous: Boolean) = VfsImplUtil.refresh(this, asynchronous)
}