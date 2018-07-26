/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.cidr.xcode.model.XcodeMetaData

class AppCodeKonanProjectComponent(project: Project): KonanProjectComponent(project) {
  override fun looksLikeKotlinNativeProject(): Boolean {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val metaData = XcodeMetaData.getInstance(project)
    return metaData.allProjects.any {
      it.getTargets<PBXTarget>(null).any {
        it.buildConfigurations.firstOrNull()?.let {
          val buildSettings = XcodeMetaData.getAllResolveConfigurationsFor(it).firstOrNull()?.buildSettings
          buildSettings?.getBuildSetting("KONAN_TASK")?.string != null
        } ?: false
      }
    }
  }
}