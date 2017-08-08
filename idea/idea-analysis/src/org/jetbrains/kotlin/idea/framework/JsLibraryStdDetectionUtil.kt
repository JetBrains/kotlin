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

import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.utils.LibraryUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*
import java.util.jar.Attributes

object JsLibraryStdDetectionUtil {
    private val IS_JS_LIBRARY_STD_LIB = Key.create<Boolean>("IS_JS_LIBRARY_STD_LIB")

    fun hasJsStdlibJar(library: Library, ignoreKind: Boolean = false): Boolean {
        if (library !is LibraryEx || library.isDisposed) return false
        if (!ignoreKind && library.kind !is JSLibraryKind) return false

        val classes = Arrays.asList(*library.getFiles(OrderRootType.CLASSES))
        return getJsStdLibJar(classes) != null
    }

    fun getJsLibraryStdVersion(library: Library): String? {
        if ((library as LibraryEx).kind !is JSLibraryKind) return null
        val jar = getJsStdLibJar(library.getFiles(OrderRootType.CLASSES).toList()) ?: return null

        return JarUtil.getJarAttribute(VfsUtilCore.virtualToIoFile(jar), Attributes.Name.IMPLEMENTATION_VERSION)
    }

    private fun getJsStdLibJar(classesRoots: List<VirtualFile>): VirtualFile? {
        for (root in classesRoots) {
            if (root.fileSystem.protocol !== StandardFileSystems.JAR_PROTOCOL) continue

            val name = root.url.substringBefore("!/").substringAfterLast('/')
            if (name == PathUtil.JS_LIB_JAR_NAME || name == PathUtil.JS_LIB_10_JAR_NAME ||
                    PathUtil.KOTLIN_STDLIB_JS_JAR_PATTERN.matcher(name).matches() ||
                    PathUtil.KOTLIN_JS_LIBRARY_JAR_PATTERN.matcher(name).matches()) {

                val jar = VfsUtilCore.getVirtualFileForJar(root) ?: continue
                var isJSStdLib = jar.getUserData(IS_JS_LIBRARY_STD_LIB)
                if (isJSStdLib == null) {
                    isJSStdLib = LibraryUtils.isKotlinJavascriptStdLibrary(File(jar.path))
                    jar.putUserData(IS_JS_LIBRARY_STD_LIB, isJSStdLib)
                }

                if (isJSStdLib) {
                    return jar
                }
            }
        }

        return null
    }
}
