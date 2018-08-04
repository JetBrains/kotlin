/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.settings

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import org.jetbrains.kotlin.konan.target.KonanTarget

class AppCodeKonanPaths(project: Project): KonanPaths(project) {
  override fun target(): KonanTarget {
    val buildConfigurations = XcodeMetaData.getInstance(project).buildConfigurations.flatMap { XcodeMetaData.getAllResolveConfigurationsFor(it) }
    val platform = buildConfigurations.asSequence().mapNotNull { it.buildSettings.baseSdk?.platform }.filter { it.isIOS || it.isMacOS }.firstOrNull()
    return if (platform?.isIOS == true) KonanTarget.IOS_ARM64 else KonanTarget.MACOS_X64
  }
}