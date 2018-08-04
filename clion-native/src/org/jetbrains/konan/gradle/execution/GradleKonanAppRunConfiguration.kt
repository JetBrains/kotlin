/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.execution.*
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.CidrBundle
import com.jetbrains.cidr.execution.*
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jdom.Element
import org.jetbrains.konan.gradle.GradleKonanWorkspace

import java.io.File
import java.util.Objects

/**
 * @author Vladislav.Soroka
 */
class GradleKonanAppRunConfiguration(project: Project,
                                     factory: ConfigurationFactory,
                                     name: String) : CidrRunConfiguration<GradleKonanConfiguration, GradleKonanBuildTarget>(project,
                                                                                                                            factory,
                                                                                                                            name), CidrExecutableDataHolder {

  private var myExecutableData: ExecutableData? = null

  private val buildProfileIfActive: String?
    get() = if (isSelectedConfiguration)
      GradleKonanBuildProfileExecutionTarget.getProfileName(ExecutionTargetManager.getInstance(project).activeTarget)
    else
      null

  internal val isSelectedConfiguration: Boolean
    get() {
      val selected = RunManager.getInstance(project).selectedConfiguration
      return selected != null && selected.uniqueID == RunnerAndConfigurationSettingsImpl.getUniqueIdFor(this)
    }

  val buildTarget: GradleKonanBuildTarget?
    get() {
      val helper = helper
      val data = targetAndConfigurationData
      return helper.findTarget(data?.target)
    }

  val buildProfiles: List<String>
    get() = ContainerUtil.map(helper.getConfigurations(buildTarget)) { it -> it.profileName }

  override fun getType(): GradleKonanAppRunConfigurationType {
    return super.getType() as GradleKonanAppRunConfigurationType
  }

  override fun canRunOn(target: ExecutionTarget): Boolean {
    return target is GradleKonanBuildProfileExecutionTarget
  }

  override fun getResolveConfiguration(target: ExecutionTarget): OCResolveConfiguration? {
    val configurations = getBuildAndRunConfigurations(target)
    return if (configurations == null)
      null
    else
      GradleKonanWorkspace.getInstance(project).getResolveConfigurationFor(configurations.buildConfiguration)
  }

  @Throws(RuntimeConfigurationException::class)
  override fun checkConfiguration() {
    super.checkConfiguration()
    doCheckConfiguration(buildProfileIfActive, false)
  }

  @Throws(RuntimeConfigurationException::class)
  override fun checkSettingsBeforeRun() {
    super.checkSettingsBeforeRun()
    doCheckConfiguration(buildProfileIfActive, true)
  }

  @Throws(RuntimeConfigurationError::class)
  private fun doCheckConfiguration(buildProfile: String?, checkExecutableSpecified: Boolean) {
    val problems = BuildConfigurationProblems()
    getBuildAndRunConfigurations(buildProfile, problems, checkExecutableSpecified)
    if (problems.hasProblems()) {
      throw RuntimeConfigurationError(problems.htmlProblems)
    }
  }

  @JvmOverloads
  fun getBuildAndRunConfigurations(target: ExecutionTarget,
                                   problems: BuildConfigurationProblems? = null,
                                   checkExecutableSpecified: Boolean = false): BuildAndRunConfigurations? {
    return getBuildAndRunConfigurations(GradleKonanBuildProfileExecutionTarget.getProfileName(target), problems, checkExecutableSpecified)
  }

  @JvmOverloads
  fun getBuildAndRunConfigurations(buildProfileName: String?,
                                   problems: BuildConfigurationProblems? = null,
                                   checkExecutableSpecified: Boolean = false): BuildAndRunConfigurations? {
    return ReadAction.compute<BuildAndRunConfigurations, RuntimeException> {
      if (project.isDisposed) return@compute null

      var buildData = targetAndConfigurationData

      // we don't use `BuildTargetAndConfigurationData.configurationName`, instead, override it with the provided `buildProfileName`
      if (buildData != null) buildData = BuildTargetAndConfigurationData(buildData.target, buildProfileName)

      val helper = GradleKonanAppRunConfigurationType.getHelper(project)
      if (!BuildTargetAndConfigurationData.checkData(helper,
                                                     buildData,
                                                     problems,
                                                     false,
                                                     true)) {
        return@compute null
      }
      assert(buildData != null)

      var runExecutable: File? = null
      var runConfig: GradleKonanConfiguration? = null
      val buildConfig: GradleKonanConfiguration?

      val runData = executableData
      if (runData == null && checkExecutableSpecified) {
        problems?.problems?.add(
          CidrBundle.message("build.configuration.parameterNotSelected", CidrBundle.message("build.configuration.executable")))
        return@compute null
      }

      var runTargetIsValid = true
      if (runData != null) {
        if (runData.target != null) {
          if (runData.target != buildData!!.target) {
            runConfig = checkAndGetConfiguration(helper, problems, runData.target!!, buildData.configurationName, true, true)
            runTargetIsValid = runConfig != null
          }
        }
        else {
          //CPPLog.LOG.assertTrue(runData.path != null);
          runExecutable = File(runData.path!!)
        }
      }

      //CPPLog.LOG.assertTrue(buildData.target != null);
      buildConfig = checkAndGetConfiguration(helper, problems, buildData!!.target!!, buildData.configurationName, false,
                                             runConfig == null && runExecutable == null)

      if (buildConfig == null || !runTargetIsValid) return@compute null

      BuildAndRunConfigurations(buildConfig!!, runConfig, runExecutable, null)
    }
  }

  private fun checkAndGetConfiguration(helper: GradleKonanBuildConfigurationHelper,
                                       problems: BuildConfigurationProblems?,
                                       dataTarget: BuildTargetData,
                                       configurationName: String?,
                                       checkTargetType: Boolean,
                                       checkProduct: Boolean): GradleKonanConfiguration? {
    val target = helper.findTarget(dataTarget)

    if (target == null || checkTargetType && !target.isExecutable) {
      problems?.problems?.add(CidrBundle.message("build.configuration.parameterNotFound",
                                                 helper.targetTitle,
                                                 dataTarget.displayString))
      return null
    }

    if (configurationName == null) return null

    val config = helper.findConfiguration(target, configurationName)
    if (config == null) {
      problems?.problems?.add(CidrBundle.message("build.configuration.parameterNotFoundForTarget",
                                                 helper.configurationTitle,
                                                 configurationName,
                                                 StringUtil.toLowerCase(helper.targetTitle),
                                                 dataTarget.displayString))
      return null
    }

    if (checkProduct && config.productFile == null) {
      problems?.problems?.add(CidrBundle.message("build.configuration.productNotFound",
                                                 StringUtil.toLowerCase(helper.targetTitle),
                                                 dataTarget.displayString,
                                                 configurationName))
      return null
    }

    return config
  }


  override fun getState(executor: Executor, env: ExecutionEnvironment): CidrCommandLineState? {
    return CidrCommandLineState(env, GradleKonanLauncher(env, this))
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    if (myExecutableData != null) myExecutableData!!.writeExternal(element)
  }

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element) {
    super.readExternal(element)
    myExecutableData = ExecutableData.loadExternal(element)
  }

  override fun getHelper(): GradleKonanBuildConfigurationHelper {
    return GradleKonanAppRunConfigurationType.getHelper(project)
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return type.createEditor(project)
  }

  override fun getExecutableData(): ExecutableData? {
    return myExecutableData
  }

  override fun setExecutableData(executableData: ExecutableData?) {
    myExecutableData = executableData
  }

  class BuildAndRunConfigurations @JvmOverloads constructor(val buildConfiguration: GradleKonanConfiguration,
                                                            runConfiguration: GradleKonanConfiguration? = null,
                                                            val runExecutable: File? = null,
                                                            val explicitBuildTargetName: String? = null) {

    val runFile: File?
      get() = runExecutable ?: runConfiguration.productFile

    val runConfiguration: GradleKonanConfiguration = runConfiguration ?: buildConfiguration
  }
}
