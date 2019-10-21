/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.testing.*
import org.jdom.Element
import org.jetbrains.konan.execution.Device
import org.jetbrains.konan.execution.MobileRunConfiguration
import java.io.File

class MobileTestRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    MobileRunConfiguration(project, factory, name),
    CidrTestRunConfiguration {

    fun getTestRunnerBundle(environment: ExecutionEnvironment): File = testRunner

    private lateinit var testData: CidrTestRunConfigurationData<MobileTestRunConfiguration>

    fun recreateTestData() {
        testData = // TODO choose based on module
            if (canRunOnAndroid) AndroidTestRunConfigurationData.FACTORY(this)
            else AppleXCTestRunConfigurationData.FACTORY(this)
    }

    init {
        recreateTestData()
    }

    override fun getTestData(): CidrTestRunConfigurationData<MobileTestRunConfiguration> = testData

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        MobileTestRunConfigurationEditor(project, helper)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState? =
        (environment.executionTarget as? Device)?.createState(this, environment)

    override fun createTestRunProfile(
        rerunFailedTestsAction: CidrRerunFailedTestsAction,
        testScope: CidrTestScope
    ): CidrRerunFailedTestsAction.CidrReturnTestProfile {
        TODO("not implemented")
    }

    override fun createLauncher(environment: ExecutionEnvironment): CidrLauncher = throw IllegalStateException()

    // TODO remove this
    var testRunner: File = File("")

    override fun writeExternal(element: Element) {
        super<MobileRunConfiguration>.writeExternal(element)
        element.setAttribute("TEST_RUNNER_PATH", testRunner.path)
        testData.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super<MobileRunConfiguration>.readExternal(element)
        recreateTestData()
        testData.readExternal(element)
        element.getAttributeValue("TEST_RUNNER_PATH")?.let { testRunner = File(it) }
    }

    override fun checkConfiguration() {
        super<MobileRunConfiguration>.checkConfiguration()
        testData.checkData()
    }

    override fun clone(): RunConfiguration {
        val result = super.clone() as MobileTestRunConfiguration
        result.testData = testData.cloneForConfiguration(result) as CidrTestRunConfigurationData<MobileTestRunConfiguration>
        return result
    }
}