package org.jetbrains.konan.tests

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.AppCodeRunConfiguration
import com.jetbrains.cidr.execution.AppCodeRunConfigurationType
import com.jetbrains.cidr.execution.testing.AppCodeTestRunConfigurationEx
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.cidr.xcode.model.XcodeMetaData

class AppCodeKonanTestRunConfigurationType: AppCodeRunConfigurationType(
  "KonanTestRunConfigurationType",
  TODO("KonanTestRunConfigurationData.FRAMEWORK_ID"),
  "Kotlin/Native test",
  "Kotlin/Native test configuration",
  { AllIcons.RunConfigurations.Junit }) {

  override fun createRunConfiguration(project: Project, factory: ConfigurationFactory): AppCodeRunConfiguration {
    return TODO("""AppCodeTestRunConfigurationEx(project, factory, "", KonanTestRunConfigurationData.FACTORY)""")
  }

  override fun createEditor(project: Project): SettingsEditor<out AppCodeRunConfiguration> {
    return TODO("KonanTestRunConfigurationEditor(project, getHelper(project))")
  }

  override fun selectTargets(all: MutableList<PBXTarget>): List<PBXTarget> {
    return all.filter { target ->
      target.isExecutable && target.buildConfigurationList.configurations.any { config ->
        XcodeMetaData.getAllResolveConfigurationsFor(config).any {
          XcodeMetaData.getBuildSettings(it)?.getBuildSetting("KONAN_TEST")?.boolean ?: false
        }
      }
    }
  }
}