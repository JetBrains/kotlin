/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms

internal class JsKlibLibraryInfo(
    project: Project,
    library: Library,
    private val libraryRoot: String
) : LibraryInfo(project, library) {

    val kotlinLibrary = resolveSingleFileKlib(File(libraryRoot))

    override fun getLibraryRoots() = listOf(libraryRoot)

    override val platform: TargetPlatform
        get() = JsPlatforms.defaultJsPlatform

    override fun toString() = "JsKlib" + super.toString()
}