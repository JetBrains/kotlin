/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import java.nio.file.Path

// FIX ME WHEN BUNCH 192 REMOVED
internal fun loadProjectCompat(projectFile: Path): Project {
    return (ProjectManagerEx.getInstanceEx()).loadProject(projectFile)
}