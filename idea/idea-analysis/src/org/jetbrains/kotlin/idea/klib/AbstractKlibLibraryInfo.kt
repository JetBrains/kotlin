/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.klib

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.util.IJLoggerAdapter
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.platform.TargetPlatform

abstract class AbstractKlibLibraryInfo(project: Project, library: Library, val libraryRoot: String) : LibraryInfo(project, library) {

    val resolvedKotlinLibrary: KotlinLibrary = resolveSingleFileKlib(
        libraryFile = File(libraryRoot),
        logger = LOG,
        strategy = ToolingSingleFileKlibResolveStrategy
    )

    val compatibilityInfo: KlibCompatibilityInfo by lazy { resolvedKotlinLibrary.getCompatibilityInfo() }

    final override fun getLibraryRoots() = listOf(libraryRoot)

    abstract override val platform: TargetPlatform // must override

    companion object {
        private val LOG = IJLoggerAdapter.getInstance(AbstractKlibLibraryInfo::class.java)
    }
}
