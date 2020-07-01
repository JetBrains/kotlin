package com.jetbrains.mobile.execution

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.CidrBuildConfigurationHelper

class MobileBuildConfigurationHelper(val project: Project) : CidrBuildConfigurationHelper<MobileBuildConfiguration, MobileBuildTarget>() {
    override fun allowEditBuildConfiguration(): Boolean = false

    override fun getTargets(): List<MobileBuildTarget> = emptyList()

    override fun findSimilarValidInTargets(
        buildTarget: MobileBuildTarget?,
        buildConfiguration: MobileBuildConfiguration?,
        buildTargets: List<MobileBuildTarget>
    ): BuildTargetAndConfigurationData? = null
}