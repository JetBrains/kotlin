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
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib

abstract class AbstractKlibLibraryInfo(project: Project, library: Library, protected val libraryRoot: String) : LibraryInfo(project, library) {

    val resolvedKotlinLibrary = resolveSingleFileKlib(
        libraryFile = File(libraryRoot),
        logger = LOG,
        strategy = ToolingSingleFileKlibResolveStrategy
    )

    val compatibilityInfo by lazy { resolvedKotlinLibrary.getCompatibilityInfo() }

    override fun getLibraryRoots() = listOf(libraryRoot)

    companion object {
        private val LOG = IJLoggerAdapter.getInstance(AbstractKlibLibraryInfo::class.java)
    }
}
