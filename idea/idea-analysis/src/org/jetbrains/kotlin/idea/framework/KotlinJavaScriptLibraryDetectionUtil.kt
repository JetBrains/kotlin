
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

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import kotlin.platform.platformStatic

public object KotlinJavaScriptLibraryDetectionUtil {

    platformStatic
    public fun isKotlinJavaScriptLibrary(library: Library): Boolean =
            isKotlinJavaScriptLibrary(library.getFiles(OrderRootType.CLASSES).toList())

    platformStatic
    public fun isKotlinJavaScriptLibrary(classesRoots: List<VirtualFile>): Boolean =
            isJsLibraryWithAcceptedFile(classesRoots) { isJsFileWithMetadata(it) }

    private fun isJsLibraryWithAcceptedFile(classesRoots: List<VirtualFile>, accept: (VirtualFile) -> Boolean): Boolean =
        JavaRuntimeDetectionUtil.getJavaRuntimeVersion(classesRoots) == null // Prevent clashing with java runtime
        && classesRoots.any { !VfsUtilCore.processFilesRecursively(it, { !accept(it) }) }

    private fun isJsFileWithMetadata(file: VirtualFile): Boolean =
            !file.isDirectory() && JavaScript.EXTENSION == file.getExtension() && KotlinJavascriptMetadataUtils.hasMetadata(String(file.contentsToByteArray(false)))
}
