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

package org.jetbrains.kotlin.idea.framework

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.caches.JarUserDataManager
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment

object CommonLibraryDetectionUtil {
    @JvmStatic
    fun getLibraryPlatform(library: Library): TargetPlatform {
        library as? LibraryEx ?: return JvmPlatform
        if (library.isDisposed) return JvmPlatform
        if (library.kind is JSLibraryKind) return JsPlatform

        for (root in library.getFiles(OrderRootType.CLASSES)) {
            ProgressManager.checkCanceled()
            val jarRoot = JarFileSystem.getInstance().getVirtualFileForJar(root) ?: root
            val hasCommonMetadata = JarUserDataManager.hasFileWithProperty(HasCommonKotlinMetadataInJar, jarRoot)
            if (hasCommonMetadata == true) {
                return TargetPlatform.Default
            }

            var platform: TargetPlatform? = null
            VfsUtilCore.processFilesRecursively(root) { file ->
                when {
                    file.fileType == JavaClassFileType.INSTANCE -> platform = JvmPlatform
                    isKotlinMetadataFile(file) -> platform = TargetPlatform.Default
                }

                platform == null
            }
            platform?.let { return it }
        }

        return JvmPlatform
    }

    private fun isKotlinMetadataFile(file: VirtualFile): Boolean =
            !file.isDirectory &&
            file.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION

    object HasCommonKotlinMetadataInJar : JarUserDataManager.JarBooleanPropertyCounter(HasCommonKotlinMetadataInJar::class.simpleName!!) {
        override fun hasProperty(file: VirtualFile) = isKotlinMetadataFile(file)
    }
}
