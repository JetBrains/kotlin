package org.jetbrains.konan.gradle.execution

import com.intellij.execution.runners.ExecutionEnvironment

class CLionGradleKonanLauncherProvider : GradleKonanLauncherProvider {

  override fun create(environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): GradleKonanLauncher {
    return CLionGradleKonanLauncher(environment, configuration)
  }

}