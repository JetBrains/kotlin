/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.platform.impl.isJavaScript
import org.jetbrains.kotlin.platform.impl.isJvm
import org.jetbrains.kotlin.platform.impl.isKotlinNative
import org.jetbrains.kotlin.idea.statistics.KotlinEventTrigger
import org.jetbrains.kotlin.idea.statistics.KotlinStatisticsTrigger

class TargetRetrieveActivity : StartupActivity {

    override fun runActivity(project: Project) {
        project.allModules().forEach {
            val buildSystem = it.getBuildSystemType()
            val platform = when {
                it.platform.isJvm -> "jvm"
                it.platform.isJavaScript -> "js"
                it.platform.isCommon -> "common"
                it.platform.isKotlinNative -> "native"
                else -> "unknown"
            }
            when {
                buildSystem == BuildSystemType.JPS ->
                    KotlinStatisticsTrigger.trigger(
                            KotlinEventTrigger.KotlinJPSTargetTrigger,
                            platform
                    )
                buildSystem.toString().toLowerCase().contains("maven") ->
                    KotlinStatisticsTrigger.trigger(
                            KotlinEventTrigger.KotlinMavenTargetTrigger,
                            platform
                    )
            }
        }
    }
}