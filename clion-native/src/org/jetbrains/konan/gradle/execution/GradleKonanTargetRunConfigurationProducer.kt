/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer

class GradleKonanTargetRunConfigurationProducer : CidrTargetRunConfigurationProducer<GradleKonanConfiguration, GradleKonanBuildTarget, GradleKonanAppRunConfiguration>(
  GradleKonanAppRunConfigurationType.instance, GradleKonanTargetRunConfigurationBinder.INSTANCE) {
  companion object {

    private var INSTANCE: GradleKonanTargetRunConfigurationProducer? = null

    @Synchronized
    fun getGradleKonanInstance(project: Project): GradleKonanTargetRunConfigurationProducer? {
      if (INSTANCE != null) {
        return INSTANCE
      }
      for (cp in RunConfigurationProducer.getProducers(project)) {
        if (cp is GradleKonanTargetRunConfigurationProducer) {
          INSTANCE = cp
          return INSTANCE
        }
      }
      return null
    }
  }
}
