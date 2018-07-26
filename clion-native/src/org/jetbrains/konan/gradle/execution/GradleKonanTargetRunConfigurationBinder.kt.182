/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.util.Pair
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationBinder
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jetbrains.konan.gradle.GradleKonanWorkspace

class GradleKonanTargetRunConfigurationBinder : CidrTargetRunConfigurationBinder<GradleKonanConfiguration, GradleKonanBuildTarget, GradleKonanAppRunConfiguration> {

  override fun isSupportedRunConfiguration(configuration: RunConfiguration): Boolean {
    return configuration is GradleKonanAppRunConfiguration
  }

  override fun getTargetAndConfiguration(runConfiguration: GradleKonanAppRunConfiguration): Pair<GradleKonanBuildTarget, GradleKonanConfiguration> {
    return Pair.create(runConfiguration.buildTarget, null)
  }

  override fun getTargetFromResolveConfiguration(configuration: OCResolveConfiguration): GradleKonanBuildTarget? {
    return GradleKonanWorkspace.getInstance(configuration.project).modelTargets.stream()
      .filter { target ->
        target.buildConfigurations.stream()
          .anyMatch { conf -> conf.id == configuration.uniqueId }
      }
      .findFirst()
      .orElse(null)
  }

  companion object {

    val INSTANCE = GradleKonanTargetRunConfigurationBinder()
  }
}
