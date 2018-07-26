/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.tests

import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.jetbrains.cidr.execution.testing.*
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestFramework

class KonanTestCommandLineState(
  configuration: CidrTestRunConfiguration,
  launcher: CidrLauncher,
  failedTests: CidrTestScope?,
  env: ExecutionEnvironment,
  executor: Executor
) : CidrTestCommandLineState<CidrTestRunConfiguration>(configuration, launcher, env, executor, failedTests, EMPTY_TEST_SCOPE_PRODUCER) {
  override fun doCreateRerunFailedTestsAction(consoleView: SMTRunnerConsoleView): CidrRerunFailedTestsAction {
    return CidrRerunFailedTestsActionEx(
      consoleView,
      { _ -> "" },
      this)
  }

  override fun createTestScopeElement(testClass: String?, testMethod: String?): CidrTestScopeElement {
    return CidrGoogleTestFramework.getInstance().createTestScopeElementForSuiteAndTest(testClass, testMethod)
  }

  override fun prepareCommandLine(cl: GeneralCommandLine): GeneralCommandLine {
    cl.addParameters(
      "--gtest_filter=" + testScope().asPattern,
      "--ktest_logger=teamcity"
    )
    return cl
  }

  companion object {
    val EMPTY_TEST_SCOPE_PRODUCER = { CidrTestScope.createEmptyTestScope(";") }
  }
}
