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

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.*
import org.jetbrains.kotlin.caches.resolve.IdePlatformKindResolution
import org.jetbrains.kotlin.caches.resolve.resolution
import org.jetbrains.kotlin.idea.vfilefinder.KnownLibraryKindForIndex
import org.jetbrains.kotlin.idea.vfilefinder.getLibraryKindForJar
import org.jetbrains.kotlin.platform.DefaultIdeTargetPlatformKindProvider
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.utils.PathUtil
import java.util.jar.Attributes
import java.util.regex.Pattern

interface KotlinLibraryKind {
    val compilerPlatform: TargetPlatform
}

object JSLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.js"), KotlinLibraryKind {
    override val compilerPlatform: TargetPlatform
        get() = DefaultBuiltInPlatforms.jsPlatform

    override fun createDefaultProperties() = DummyLibraryProperties.INSTANCE!!
}

object CommonLibraryKind : PersistentLibraryKind<DummyLibraryProperties>("kotlin.common"), KotlinLibraryKind {
    override val compilerPlatform: TargetPlatform
        get() = DefaultBuiltInPlatforms.commonPlatform

    override fun createDefaultProperties() = DummyLibraryProperties.INSTANCE!!
}

val PersistentLibraryKind<*>?.platform: TargetPlatform
    get() = when (this) {
        is KotlinLibraryKind -> this.compilerPlatform
        else -> DefaultIdeTargetPlatformKindProvider.defaultCompilerPlatform
    }

fun getLibraryPlatform(project: Project, library: Library): TargetPlatform {
    if (library !is LibraryEx) return DefaultIdeTargetPlatformKindProvider.defaultCompilerPlatform
    if (library.isDisposed) return DefaultIdeTargetPlatformKindProvider.defaultCompilerPlatform

    return library.effectiveKind(project).platform
}

fun detectLibraryKind(roots: Array<VirtualFile>): PersistentLibraryKind<*>? {
    val jarFile = roots.firstOrNull() ?: return null
    if (jarFile.fileSystem is JarFileSystem) {
        // TODO: Detect library kind for Jar file using IdePlatformKindResolution.
        when (jarFile.getLibraryKindForJar()) {
            KnownLibraryKindForIndex.COMMON -> return CommonLibraryKind
            KnownLibraryKindForIndex.JS -> return JSLibraryKind
            KnownLibraryKindForIndex.UNKNOWN -> {
                /* Continue detection of library kind via IdePlatformKindResolution. */
            }
        }
    }

    val matchingResolution =
        IdePlatformKindResolution
            .getInstances()
            .firstOrNull { it.isLibraryFileForPlatform(jarFile) }

    if (matchingResolution != null) return matchingResolution.libraryKind

    return DefaultIdeTargetPlatformKindProvider.defaultPlatform.kind.resolution.libraryKind
}

fun getLibraryJar(roots: Array<VirtualFile>, jarPattern: Pattern): VirtualFile? {
    return roots.firstOrNull { jarPattern.matcher(it.name).matches() }
}

fun getLibraryJarVersion(library: Library, jarPattern: Pattern): String? {
    val libraryJar = getLibraryJar(library.getFiles(OrderRootType.CLASSES), jarPattern) ?: return null
    return JarUtil.getJarAttribute(VfsUtilCore.virtualToIoFile(libraryJar), Attributes.Name.IMPLEMENTATION_VERSION)
}

fun getCommonRuntimeLibraryVersion(library: Library) = getLibraryJarVersion(library, PathUtil.KOTLIN_STDLIB_COMMON_JAR_PATTERN)
