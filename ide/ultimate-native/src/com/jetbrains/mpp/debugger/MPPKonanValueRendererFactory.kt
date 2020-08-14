/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.intellij.openapi.project.Project
import com.jetbrains.mpp.debugger.KonanValueRendererFactory
import com.jetbrains.mpp.MPPWorkspace

class MPPKonanValueRendererFactory : KonanValueRendererFactory() {
    override fun getWorkspace(project: Project) = MPPWorkspace.getInstance(project)
}