/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable

fun Project.invalidateProjectRoots() {
    ProjectRootManagerEx.getInstanceEx(this).makeRootsChange(EmptyRunnable.INSTANCE, false, true)
}