/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper

class MobileBuildConfigurationHelper(val project: Project) : CidrBuildConfigurationHelper<MobileBuildConfiguration, MobileBuildTarget>() {
    override fun allowEditBuildConfiguration(): Boolean = false

    override fun getTargets(): List<MobileBuildTarget> {
        // TODO retrieve from external system project tree
        return emptyList()
    }

    override fun <T : MobileBuildTarget> findTarget(data: BuildTargetData?, buildTargets: List<T>): T? =
        if (data == null) null else
            buildTargets.find { it.projectName == data.projectName && it.name == data.targetName }

    override fun getDefaultConfiguration(buildTarget: MobileBuildTarget?): MobileBuildConfiguration? =
        getConfigurations(buildTarget).firstOrNull()

    override fun findSimilarValidInTargets(
        buildTarget: MobileBuildTarget?,
        buildConfiguration: MobileBuildConfiguration?,
        buildTargets: List<MobileBuildTarget>
    ): BuildTargetAndConfigurationData? = null
}