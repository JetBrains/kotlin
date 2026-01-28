/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope

// TODO: limit this scope to the Java source roots, which the module has in its CONTENT_ROOTS
class AllJavaSourcesInProjectScope(project: Project) : DelegatingGlobalSearchScope(allScope(project)) {
    // 'isDirectory' check is needed because otherwise directories such as 'frontend.java' would be recognized
    // as Java source files, which makes no sense
    override fun contains(file: VirtualFile) =
        (file.extension == JavaFileType.DEFAULT_EXTENSION || file.fileType === JavaFileType.INSTANCE) && !file.isDirectory

    override fun toString() = "All Java sources in the project"
}
