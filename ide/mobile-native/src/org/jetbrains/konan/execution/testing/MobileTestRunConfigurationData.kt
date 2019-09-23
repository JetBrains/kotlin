/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution.testing

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.jetbrains.cidr.execution.testing.CidrTestRunConfigurationData
import com.jetbrains.cidr.execution.testing.CidrTestScope

class MobileJUnitRunConfigurationData(configuration: MobileTestRunConfiguration) :
    CidrTestRunConfigurationData<MobileTestRunConfiguration>(configuration) {

    override fun getTestingFrameworkId(): String = "JUnit"

    override fun formatTestMethod(): String = "$testSuite.$testName"

    override fun createTestConsoleProperties(executor: Executor, executionTarget: ExecutionTarget): SMTRunnerConsoleProperties =
        AndroidTestConsoleProperties(myConfiguration, executor)

    override fun createState(environment: ExecutionEnvironment, executor: Executor, testScope: CidrTestScope?): CommandLineState {
        TODO("not implemented")
    }

    override fun checkData() {}
}