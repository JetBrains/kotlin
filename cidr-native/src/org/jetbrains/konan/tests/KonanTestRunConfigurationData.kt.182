/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.tests

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.testing.CidrTestRunConfiguration
import com.jetbrains.cidr.execution.testing.CidrTestRunConfigurationData
import com.jetbrains.cidr.execution.testing.CidrTestScope

class KonanTestRunConfigurationData(configuration: CidrTestRunConfiguration) : CidrTestRunConfigurationData<CidrTestRunConfiguration>(
  configuration) {
  override fun checkData() {
  }

  override fun getTestingFrameworkId() = FRAMEWORK_ID

  override fun formatTestMethod(): String = ""

  override fun createState(env: ExecutionEnvironment, executor: Executor, failedTests: CidrTestScope?): CidrCommandLineState =
    KonanTestCommandLineState(myConfiguration, myConfiguration.createLauncher(env), failedTests, env, executor)

  override fun createTestConsoleProperties(executor: Executor, target: ExecutionTarget): SMTRunnerConsoleProperties =
    KonanTestConsoleProperties(myConfiguration, executor, target)

  companion object {
    val FACTORY = { connection: CidrTestRunConfiguration -> KonanTestRunConfigurationData(connection) }
    val FRAMEWORK_ID: String = "KonanTestFramework"
  }
}
