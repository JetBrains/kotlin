
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
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.caches.JarUserDataIndex
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import kotlin.platform.platformStatic

public object KotlinJavaScriptLibraryDetectionUtil {
    platformStatic
    public fun isKotlinJavaScriptLibrary(library: Library): Boolean =
            isKotlinJavaScriptLibrary(library.getFiles(OrderRootType.CLASSES).toList())

    platformStatic
    public fun isKotlinJavaScriptLibrary(classesRoots: List<VirtualFile>): Boolean {
        // Prevent clashing with java runtime
        if (JavaRuntimeDetectionUtil.getJavaRuntimeVersion(classesRoots) != null) return false

        classesRoots.forEach { root ->
            val cachedResult = JarUserDataIndex.getValue(HasKotlinJSMetadataInJar, root)

            @suppress("NON_EXHAUSTIVE_WHEN")
            when (cachedResult) {
                HasKotlinJSMetadataInJar.JsMetadataState.HAS_JS_METADATA -> return true
                HasKotlinJSMetadataInJar.JsMetadataState.NO_JS_METADATA -> return false
            }

            if (!VfsUtilCore.processFilesRecursively(root, { isJsFileWithMetadata(root) })) {
                return true
            }
        }

        return false
    }

    private fun isJsFileWithMetadata(file: VirtualFile): Boolean =
            !file.isDirectory() &&
            JavaScript.EXTENSION == file.getExtension() &&
            KotlinJavascriptMetadataUtils.hasMetadata(String(file.contentsToByteArray(false)))

    public object HasKotlinJSMetadataInJar : JarUserDataIndex.JarUserDataCollector<HasKotlinJSMetadataInJar.JsMetadataState> {
        public enum class JsMetadataState {
            HAS_JS_METADATA,
            NO_JS_METADATA,
            COUNTING
        }

        override val key = Key.create<HasKotlinJSMetadataInJar.JsMetadataState>(HasKotlinJSMetadataInJar::class.simpleName!!)

        override val init = JsMetadataState.COUNTING
        override val stopState = JsMetadataState.HAS_JS_METADATA
        override val notFoundState = JsMetadataState.NO_JS_METADATA

        override val sdk = JsMetadataState.NO_JS_METADATA

        override fun count(file: VirtualFile): JsMetadataState {
            return if (KotlinJavaScriptLibraryDetectionUtil.isJsFileWithMetadata(file)) {
                JsMetadataState.HAS_JS_METADATA
            } else {
                JsMetadataState.NO_JS_METADATA
            }
        }

        override fun state(str: String) = JsMetadataState.valueOf(str)
    }
}
