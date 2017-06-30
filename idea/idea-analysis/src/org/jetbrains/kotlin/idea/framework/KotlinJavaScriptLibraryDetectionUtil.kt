
/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.caches.JarUserDataManager
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.util.zip.ZipException

object KotlinJavaScriptLibraryDetectionUtil {
    @JvmStatic fun isKotlinJavaScriptLibrary(library: Library): Boolean {
        if (library is LibraryEx && library.isDisposed) return false

        return isKotlinJavaScriptLibrary(library.getFiles(OrderRootType.CLASSES).toList())
    }

    @JvmStatic fun isKotlinJavaScriptLibrary(classesRoots: List<VirtualFile>): Boolean {
        // Prevent clashing with java runtime
        if (JavaRuntimeDetectionUtil.getRuntimeJar(classesRoots) != null) return false

        classesRoots.forEach { root ->
            ProgressManager.checkCanceled()
            val hasMetadata = HasKotlinJSMetadataInJar.hasMetadataFromCache(root)
            if (hasMetadata != null) {
                return hasMetadata
            }

            if (!VfsUtilCore.processFilesRecursively(root) { !isJsFileWithMetadata(it) }) {
                return true
            }
        }

        return false
    }

    fun isJsFileWithMetadata(file: VirtualFile): Boolean {
        if (!file.isDirectory && JavaScript.EXTENSION == file.extension) {
            val content = try {
                file.contentsToByteArray(false)
            }
            catch (e: ZipException) {
                throw RuntimeException("file:${file.path} isDirectory: ${file.isDirectory}, exists: ${file.exists()}", e)
            }

            return KotlinJavascriptMetadataUtils.hasMetadata(String(content))
        }

        return false
    }

    object HasKotlinJSMetadataInJar : JarUserDataManager.JarBooleanPropertyCounter(HasKotlinJSMetadataInJar::class.simpleName!!) {
        override fun hasProperty(file: VirtualFile) = KotlinJavaScriptLibraryDetectionUtil.isJsFileWithMetadata(file)

        fun hasMetadataFromCache(root: VirtualFile): Boolean? = JarUserDataManager.hasFileWithProperty(HasKotlinJSMetadataInJar, root)
    }
}
