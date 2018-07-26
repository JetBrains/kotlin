/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.util.ui.GridBag
import com.jetbrains.cidr.execution.CidrRunConfigurationExecutableEditor
import com.jetbrains.cidr.execution.CidrRunConfigurationSettingsEditor
import javax.swing.JPanel

/**
 * @author Vladislav.Soroka
 */
class GradleKonanAppRunConfigurationSettingsEditor(project: Project,
                                                   configHelper: GradleKonanBuildConfigurationHelper) : CidrRunConfigurationSettingsEditor<GradleKonanConfiguration, GradleKonanBuildTarget, GradleKonanAppRunConfiguration, GradleKonanBuildConfigurationHelper>(project, configHelper) {

  private var myExecutableEditor: CidrRunConfigurationExecutableEditor<GradleKonanConfiguration, GradleKonanBuildTarget, GradleKonanAppRunConfiguration, GradleKonanBuildConfigurationHelper>? = null

  override fun createAdditionalControls(panel: JPanel, g: GridBag) {
    super.createAdditionalControls(panel, g)
    myExecutableEditor = CidrRunConfigurationExecutableEditor(myProject, myConfigHelper, hasTargetsInSeveralProjects())
    myExecutableEditor!!.createAdditionalControls(panel, g)
  }

  override fun onTargetSelected(target: GradleKonanBuildTarget?) {
    super.onTargetSelected(target)
    myExecutableEditor!!.onTargetSelected(target)
  }

  @Throws(ConfigurationException::class)
  override fun applyEditorTo(runConfiguration: GradleKonanAppRunConfiguration) {
    super.applyEditorTo(runConfiguration)
    myExecutableEditor!!.applyEditorTo(runConfiguration)
  }

  override fun resetEditorFrom(runConfiguration: GradleKonanAppRunConfiguration) {
    super.resetEditorFrom(runConfiguration)
    myExecutableEditor!!.resetEditorFrom(runConfiguration)
  }
}
