/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class TestGradleScriptListener(project: Project) : GradleScriptListener(project) {
    override fun isApplicable(vFile: VirtualFile): Boolean {
        return true
    }
}