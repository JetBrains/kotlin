package com.jetbrains.mobile.execution

import com.jetbrains.cidr.execution.CidrBuildConfiguration
import com.jetbrains.cidr.execution.CidrBuildTarget
import javax.swing.Icon

class MobileBuildConfiguration : CidrBuildConfiguration {
    override fun getName(): String = "Mobile"
}

class MobileBuildTarget(
    private val name: String,
    private val projectName: String,
    private val buildConfigurations: List<MobileBuildConfiguration>
) : CidrBuildTarget<MobileBuildConfiguration> {
    override fun getName(): String = name
    override fun getProjectName(): String = projectName
    override fun getBuildConfigurations(): List<MobileBuildConfiguration> = buildConfigurations
    override fun getIcon(): Icon? = null
    override fun isExecutable(): Boolean = true
}