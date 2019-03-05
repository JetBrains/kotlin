/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.execution.build.CLionBuildConfigurationProvider
import org.jetbrains.konan.gradle.GradleKonanWorkspace

class CLionKonanBuildConfigurationProvider : CLionBuildConfigurationProvider {

    override fun getBuildableConfigurations(project: Project): List<GradleKonanConfiguration> {
        val workspace = GradleKonanWorkspace.getInstance(project)
        if (!workspace.isInitialized) return emptyList()

        return workspace.buildTargets.flatMap { it.buildConfigurations }
    }
}
