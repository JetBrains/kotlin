/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger

class TargetRetrieveActivity : StartupActivity {

    override fun runActivity(project: Project) {
        project.allModules().forEach {
            val buildSystem = it.getBuildSystemType()
            // TODO(dsavvinov): review that
            val platform = when {
                it.platform.isJvm() -> "jvm"
                it.platform.isJs() -> "js"
                it.platform.isCommon() -> "common"
                it.platform.isNative() -> "native"
                else -> "unknown"
            }
            when {
                buildSystem == BuildSystemType.JPS ->
                    KotlinFUSLogger.log(
                        FUSEventGroups.JPSTarget,
                        platform
                    )
                buildSystem.toString().toLowerCase().contains("maven") ->
                    KotlinFUSLogger.log(
                        FUSEventGroups.MavenTarget,
                        platform
                    )
            }
        }
    }
}