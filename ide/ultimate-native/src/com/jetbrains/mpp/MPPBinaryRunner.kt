/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.openapi.project.Project
import com.jetbrains.konan.KonanBundle

class MPPBinaryRunner : BinaryDebugRunner() {
    override fun getRunnerId(): String = KonanBundle.message("id.runner")
    override fun getWorkspace(project: Project) = MPPWorkspace.getInstance(project)
}