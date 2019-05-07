/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper
import org.jetbrains.konan.gradle.GradleKonanWorkspace

/**
 * @author Vladislav.Soroka
 */
class GradleKonanBuildConfigurationHelper(private val myProject: Project) :
    CidrBuildConfigurationHelper<GradleKonanConfiguration, GradleKonanBuildTarget>() {

    override fun allowEditBuildConfiguration() = false

    override fun getTargets() = GradleKonanWorkspace.getInstance(myProject).buildTargets

    override fun getDefaultConfiguration(target: GradleKonanBuildTarget?) = getConfigurations(target).firstOrNull()

    override fun getRunTargets() = targets.filter { it.isExecutable }

    override fun findRunTarget(data: BuildTargetData?) = findTarget(data, runTargets)

    @Suppress("FINAL_UPPER_BOUND")
    override fun <T : GradleKonanBuildTarget> findTarget(targetData: BuildTargetData?, targets: List<T>): T? {
        if (targetData == null) return null
        return targets.firstOrNull { it.projectName == targetData.projectName && it.name == targetData.targetName }
    }

    override fun findConfiguration(target: GradleKonanBuildTarget?, name: String?): GradleKonanConfiguration? {
        if (name == null) return null
        return getConfigurations(target).firstOrNull { it.profileName == name }
    }

    override fun findSimilarValidInTargets(
        selectedTarget: GradleKonanBuildTarget?,
        selectedConfiguration: GradleKonanConfiguration?,
        targetsWithContext: List<GradleKonanBuildTarget>
    ) = if (targetsWithContext.contains(selectedTarget)) BuildTargetAndConfigurationData(selectedTarget, null as String?) else null
}
