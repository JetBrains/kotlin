package com.jetbrains.mobile.execution

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SmartList
import com.jetbrains.cidr.execution.CidrRunConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.mobile.MobileBundle
import com.jetbrains.mobile.isMobileAppMain
import org.jdom.Element
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File

abstract class MobileRunConfigurationBase(project: Project, factory: ConfigurationFactory, name: String) :
    CidrRunConfiguration<MobileBuildConfiguration, MobileBuildTarget>(project, factory, name), MobileRunConfiguration {

    private var _module = RunConfigurationModule(project).also {
        it.module = project.allModules().firstOrNull { module -> isSuitable(module) }
    }
    var module: Module?
        get() = _module.module
        set(value) {
            _module.module = value
        }

    // Storing names instead of IDs to make it more convenient for sharing run configurations
    var executionTargetNames: List<String> = emptyList()

    val executionTargets: List<Device>
        get() = MobileDeviceService.getInstance(project).getAll()
            .filter { it.displayName in executionTargetNames }

    protected abstract fun isSuitable(module: Module): Boolean

    override fun getProductBundle(device: Device): File {
        val moduleRoot = ExternalSystemApiUtil.getExternalProjectPath(module!!)?.let { File(it) }
            ?: throw IllegalStateException()
        val binaryName = GradleUtil.findGradleModuleData(module!!)?.data?.externalName
            ?: throw IllegalStateException()

        return when (device) {
            is AndroidDevice -> {
                File(moduleRoot, FileUtil.join("build", "outputs", "apk", "debug", "$binaryName-debug.apk"))
            }
            is AppleDevice -> {
                @Suppress("SpellCheckingInspection")
                val deviceType = when (device) {
                    is ApplePhysicalDevice -> "iphoneos"
                    is AppleSimulator -> "iphonesimulator"
                }
                File(moduleRoot, FileUtil.join("build", "bin", "iosAppMain", "Debug-$deviceType", "$binaryName.app"))
            }
            else -> throw IllegalStateException()
        }
    }

    private val helper = MobileBuildConfigurationHelper(project)
    override fun getHelper(): MobileBuildConfigurationHelper = helper

    override fun getResolveConfiguration(target: ExecutionTarget): OCResolveConfiguration? = null

    override fun writeExternal(element: Element) {
        super<CidrRunConfiguration>.writeExternal(element)
        _module.writeExternal(element)

        val devicesElement = Element(DEVICES_ELEMENT)
        for (deviceName in executionTargetNames) {
            val deviceElement = Element(DEVICE_ELEMENT)
                .setAttribute(DEVICE_NAME_ATTRIBUTE, deviceName)
            devicesElement.addContent(deviceElement)
        }
        element.addContent(devicesElement)
    }

    override fun readExternal(element: Element) {
        super<CidrRunConfiguration>.readExternal(element)
        _module.readExternal(element)

        val deviceElements = element.getChild(DEVICES_ELEMENT)?.getChildren(DEVICE_ELEMENT) ?: emptyList()
        executionTargetNames = deviceElements.mapNotNull { it.getAttributeValue(DEVICE_NAME_ATTRIBUTE) }
    }

    override fun clone(): MobileRunConfigurationBase {
        val result = super.clone() as MobileRunConfigurationBase
        result._module = RunConfigurationModule(project).also { it.module = this.module }
        result.executionTargetNames = executionTargetNames.toMutableList()
        return result
    }

    override fun checkConfiguration() {
        super<CidrRunConfiguration>.checkConfiguration()
        _module.checkForWarning()
        if (executionTargetNames.isEmpty()) {
            throw RuntimeConfigurationError(MobileBundle.message("device.not.selected"))
        }
    }

    companion object {
        private val log = logger<MobileRunConfigurationBase>()

        private const val DEVICES_ELEMENT = "devices"
        private const val DEVICE_ELEMENT = "device"
        private const val DEVICE_NAME_ATTRIBUTE = "display-name"
    }
}

class MobileAppRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    MobileRunConfigurationBase(project, factory, name) {

    override fun isSuitable(module: Module): Boolean = module.isMobileAppMain

    override fun getState(executor: Executor, environment: ExecutionEnvironment): CommandLineState {
        val states = SmartList<CommandLineState>()
        val appleDevice = executionTargets.filterIsInstance<AppleDevice>().singleOrNull()
        if (appleDevice != null) {
            states += createAppleState(environment, executor, appleDevice)
        }
        val androidDevice = executionTargets.filterIsInstance<AndroidDevice>().singleOrNull()
        if (androidDevice != null) {
            states += AndroidAppCommandLineState(this, androidDevice, environment)
        }
        return CompositeCommandLineState(environment, states)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        MobileRunConfigurationEditor(project, ::isSuitable)
}