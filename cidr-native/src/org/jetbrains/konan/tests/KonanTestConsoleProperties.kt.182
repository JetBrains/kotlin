/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.tests

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestConsoleProperties

class KonanTestConsoleProperties(config: RunConfiguration, executor: Executor, target: ExecutionTarget)
  : CidrGoogleTestConsoleProperties(config, executor, target) {
  override fun createTestEventsConverter(testFrameworkName: String,
                                         consoleProperties: TestConsoleProperties): OutputToGeneralTestEventsConverter {
    return OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties)
  }

  override fun getTestLocator(): SMTestLocator = KonanTestLocator
}