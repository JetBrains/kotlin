/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.LibraryKind

class CommonStandardLibraryDescription(project: Project?) : CustomLibraryDescriptorWithDeferredConfig(
    // TODO: KotlinCommonModuleConfigurator
    project, "common", LIBRARY_NAME, DIALOG_TITLE, LIBRARY_CAPTION, KOTLIN_COMMON_STDLIB_KIND, SUITABLE_LIBRARY_KINDS
) {
    companion object {
        val KOTLIN_COMMON_STDLIB_KIND = LibraryKind.create("kotlin-stdlib-common")
        val LIBRARY_NAME = "KotlinStdlibCommon"

        val DIALOG_TITLE = "Create Kotlin Common Standard Library"
        val LIBRARY_CAPTION = "Kotlin Common Standard Library"
        val SUITABLE_LIBRARY_KINDS: Set<LibraryKind> = setOf(KOTLIN_COMMON_STDLIB_KIND)
    }
}
