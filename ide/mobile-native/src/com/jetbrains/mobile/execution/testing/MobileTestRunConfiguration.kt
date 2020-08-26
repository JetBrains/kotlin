package com.jetbrains.mobile.execution.testing

import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrLauncher
import com.jetbrains.cidr.execution.testing.*
import com.jetbrains.mobile.execution.*
import com.jetbrains.mobile.isCommonTest
import org.jdom.Element
import java.io.File

class MobileTestRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    MobileRunConfigurationBase(project, factory, name),
    CidrTestRunConfiguration {

    fun getTestRunnerBundle(device: Device): File {
        val app = getProductBundle(device)
        val appName = app.nameWithoutExtension
        return when (device) {
            is AppleDevice -> File(File(app, "PlugIns"), "${appName}Tests.xctest")
            is AndroidDevice -> File(File(File(app.parentFile.parentFile, "androidTest"), app.parentFile.name), "$appName-androidTest.apk")
            else -> throw IllegalStateException()
        }
    }

    private var testData = CompositeTestRunConfigurationData(this, listOf(AppleXCTestRunConfigurationData(this), AndroidTestRunConfigurationData(this)))
    override fun getTestData(): CidrTestRunConfigurationData<MobileTestRunConfiguration> = testData

    override fun isSuitable(module: Module): Boolean = module.isCommonTest

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState? =
        testData.createState(environment, executor, null)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        MobileTestRunConfigurationEditor(project, ::isSuitable)

    override fun createLauncher(environment: ExecutionEnvironment, device: AppleDevice): CidrLauncher =
        AppleXCTestLauncher(this, environment, device)

    override fun createLauncher(environment: ExecutionEnvironment): CidrLauncher = throw IllegalStateException()

    override fun writeExternal(element: Element) {
        super<MobileRunConfigurationBase>.writeExternal(element)
        testData.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super<MobileRunConfigurationBase>.readExternal(element)
        testData.readExternal(element)
    }

    override fun checkConfiguration() {
        super<MobileRunConfigurationBase>.checkConfiguration()
        testData.checkData()
    }

    override fun clone(): MobileTestRunConfiguration {
        val result = super.clone() as MobileTestRunConfiguration
        result.testData = testData.cloneForConfiguration(result)
        return result
    }
}