package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationBinder
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration

object MobileBuildTargetRunConfigurationBinder : CidrTargetRunConfigurationBinder<
        MobileBuildConfiguration,
        MobileBuildTarget,
        MobileRunConfigurationBase> {

    override fun isSupportedRunConfiguration(configuration: RunConfiguration): Boolean =
        configuration is MobileRunConfiguration

    private fun getTargetAndConfiguration(project: Project): Pair<MobileBuildTarget, MobileBuildConfiguration> {
        val helper = MobileBuildConfigurationHelper(project)
        val target = helper.targets.first()
        return Pair.create(target, target.buildConfigurations.first())
    }

    override fun getTargetAndConfiguration(configuration: MobileRunConfigurationBase): Pair<MobileBuildTarget, MobileBuildConfiguration> =
        getTargetAndConfiguration(configuration.project)

    override fun getTargetFromResolveConfiguration(resolveConfiguration: OCResolveConfiguration): MobileBuildTarget? =
        getTargetAndConfiguration(resolveConfiguration.project).first
}
