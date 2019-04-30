package org.jetbrains.konan.gradle.execution

import com.intellij.execution.runners.ExecutionEnvironment

class AppCodeGradleKonanLauncherProvider : GradleKonanLauncherProvider {

  override fun create(environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): GradleKonanLauncher {
    return AppCodeGradleKonanLauncher(environment, configuration)
  }

}