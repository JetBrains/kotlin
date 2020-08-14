/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios.execution

import com.intellij.openapi.project.Project
import com.jetbrains.kmm.ios.ProjectWorkspace
import com.jetbrains.mpp.debugger.KonanValueRendererFactory

class KmmKonanValueRendererFactory : KonanValueRendererFactory() {
    override fun getWorkspace(project: Project) = ProjectWorkspace.getInstance(project)
}