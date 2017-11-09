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

import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.*
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import org.jetbrains.kotlin.utils.PathUtil
import java.util.jar.Attributes
import java.util.regex.Pattern

object JSLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.js") {
    override fun createDefaultProperties() = DummyLibraryProperties.INSTANCE!!
}

object CommonLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.common") {
    override fun createDefaultProperties() = DummyLibraryProperties.INSTANCE!!
}

fun getLibraryPlatform(library: Library): TargetPlatform {
    library as? LibraryEx ?: return JvmPlatform
    if (library.isDisposed) return JvmPlatform

    return when (library.kind) {
        JSLibraryKind -> JsPlatform
        CommonLibraryKind -> TargetPlatform.Common
        else -> JvmPlatform
    }
}

fun detectLibraryKind(roots: Array<VirtualFile>): PersistentLibraryKind<*>? {
    val jarFile = roots.firstOrNull() ?: return null
    if (jarFile.fileSystem is JarFileSystem) {
        return detectLibraryKindFromJarContents(jarFile)
    }

    return when (jarFile.extension) {
        "js", "kjsm" -> JSLibraryKind
        MetadataPackageFragment.METADATA_FILE_EXTENSION -> CommonLibraryKind
        else -> null
    }
}

private fun detectLibraryKindFromJarContents(jarRoot: VirtualFile): PersistentLibraryKind<*>? {
    var result: PersistentLibraryKind<*>? = null
    VfsUtil.visitChildrenRecursively(jarRoot, object : VirtualFileVisitor<PersistentLibraryKind<*>>() {
        override fun visitFile(file: VirtualFile): Boolean =
                when (file.extension) {
                    "class" -> false

                    "kjsm" -> {
                        result = JSLibraryKind
                        false
                    }

                    MetadataPackageFragment.METADATA_FILE_EXTENSION -> {
                        result = CommonLibraryKind
                        false
                    }

                    else -> true
                }
    })
    return result
}

fun getLibraryJarVersion(library: Library, jarPattern: Pattern): String? {
    for (file in library.getFiles(OrderRootType.CLASSES)) {
        if (jarPattern.matcher(file.name).matches()) {
            return JarUtil.getJarAttribute(VfsUtilCore.virtualToIoFile(file), Attributes.Name.IMPLEMENTATION_VERSION)
        }
    }
    return null
}

fun getCommonRuntimeLibraryVersion(library: Library) =
        getLibraryJarVersion(library, PathUtil.KOTLIN_STDLIB_COMMON_JAR_PATTERN)
