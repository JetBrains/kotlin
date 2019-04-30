/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.extensions.ExtensionPointName

interface GradleKonanLauncherProvider {

  fun create(environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): GradleKonanLauncher

  companion object {
    private val EP_NAME = ExtensionPointName.create<GradleKonanLauncherProvider>("org.jetbrains.kotlin.native.gradleLauncherProvider")

    fun create(environment: ExecutionEnvironment, configuration: GradleKonanAppRunConfiguration): GradleKonanLauncher {
      return EP_NAME.extensions.first().create(environment, configuration)
    }
  }

}