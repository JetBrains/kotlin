/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.project.Project
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper
import org.jetbrains.konan.gradle.GradleKonanWorkspace

/**
 * @author Vladislav.Soroka
 */
class GradleKonanBuildConfigurationHelper(private val myProject: Project) : CidrBuildConfigurationHelper<GradleKonanConfiguration, GradleKonanBuildTarget>() {

  override fun allowEditBuildConfiguration(): Boolean {
    return false
  }

  override fun getTargets(): List<GradleKonanBuildTarget> {
    return GradleKonanWorkspace.getInstance(myProject).modelTargets
  }

  override fun getDefaultConfiguration(target: GradleKonanBuildTarget?): GradleKonanConfiguration? {
    return ContainerUtil.getFirstItem(getConfigurations(target))
  }

  override fun getRunTargets(): List<GradleKonanBuildTarget> {
    return ContainerUtil.filter(targets) { target -> target.isExecutable }
  }

  override fun findRunTarget(data: BuildTargetData?): GradleKonanBuildTarget? {
    return findTarget(data, runTargets)
  }

  override fun <T : GradleKonanBuildTarget> findTarget(targetData: BuildTargetData?, targets: List<T>): T? {
    if (targetData == null) return null
    for (each in targets) {
      if (each.projectName == targetData.projectName && each.name == targetData.targetName) return each
    }
    return null
  }

  override fun findConfiguration(target: GradleKonanBuildTarget?, name: String?): GradleKonanConfiguration? {
    if (name == null) return null
    for (each in getConfigurations(target)) {
      if (each.profileName == name) return each
    }
    return null
  }

  override fun findSimilarValidInTargets(selectedTarget: GradleKonanBuildTarget?,
                                         selectedConfiguration: GradleKonanConfiguration?,
                                         targetsWithContext: List<GradleKonanBuildTarget>): BuildTargetAndConfigurationData? {
    return if (targetsWithContext.contains(selectedTarget)) BuildTargetAndConfigurationData(selectedTarget, null as String?) else null
  }
}
