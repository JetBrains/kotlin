package org.jetbrains.konan.gradle.execution

import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrTargetRunConfigurationProducer

class GradleKonanTargetRunConfigurationProducer
    : CidrTargetRunConfigurationProducer<GradleKonanConfiguration, GradleKonanBuildTarget, GradleKonanAppRunConfiguration>(
        GradleKonanTargetRunConfigurationBinder
) {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return GradleKonanAppRunConfigurationType.instance.factory
    }

    companion object {

        private var INSTANCE: GradleKonanTargetRunConfigurationProducer? = null

        @Synchronized
        fun getGradleKonanInstance(project: Project): GradleKonanTargetRunConfigurationProducer? {
            if (INSTANCE != null) {
                return INSTANCE
            }
            for (cp in RunConfigurationProducer.getProducers(project)) {
                if (cp is GradleKonanTargetRunConfigurationProducer) {
                    INSTANCE = cp
                    return INSTANCE
                }
            }
            return null
        }
    }
}
