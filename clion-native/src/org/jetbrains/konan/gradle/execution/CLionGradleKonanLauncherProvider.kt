/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.runners.ExecutionEnvironment

class CLionGradleKonanLauncherProvider : GradleKonanLauncherProvider {

  override fun create(environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): GradleKonanLauncher {
    return CLionGradleKonanLauncher(environment, configuration)
  }

}