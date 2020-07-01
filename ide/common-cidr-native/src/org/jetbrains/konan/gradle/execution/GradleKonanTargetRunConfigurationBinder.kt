package org.jetbrains.konan.gradle.execution

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.util.Pair
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationBinder
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jetbrains.konan.gradle.GradleKonanWorkspace

object GradleKonanTargetRunConfigurationBinder :
    CidrTargetRunConfigurationBinder<GradleKonanConfiguration, GradleKonanBuildTarget, GradleKonanAppRunConfiguration> {

    override fun isSupportedRunConfiguration(configuration: RunConfiguration) = configuration is GradleKonanAppRunConfiguration

    override fun getTargetAndConfiguration(runConfiguration: GradleKonanAppRunConfiguration) =
        Pair.create<GradleKonanBuildTarget, GradleKonanConfiguration>(runConfiguration.buildTarget, null)

    override fun getTargetFromResolveConfiguration(configuration: OCResolveConfiguration): GradleKonanBuildTarget? {
        return GradleKonanWorkspace.getInstance(configuration.project).buildTargets.firstOrNull { target ->
            target.buildConfigurations.any { conf -> conf.id == configuration.uniqueId }
        }
    }
}
