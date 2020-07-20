package com.jetbrains.mobile.execution

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationModule
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.execution.CidrRunConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.mobile.isAndroid
import com.jetbrains.mobile.isApple
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

    protected abstract fun isSuitable(module: Module): Boolean

    override fun canRunOn(target: ExecutionTarget): Boolean =
        target is Device &&
                (module?.isApple == true && target is AppleDevice) ||
                (module?.isAndroid == true && target is AndroidDevice)

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
    }

    override fun readExternal(element: Element) {
        super<CidrRunConfiguration>.readExternal(element)
        _module.readExternal(element)
    }

    override fun clone(): MobileRunConfigurationBase {
        val result = super.clone() as MobileRunConfigurationBase
        result._module = RunConfigurationModule(project).also { it.module = this.module }
        return result
    }

    override fun checkConfiguration() {
        super<CidrRunConfiguration>.checkConfiguration()
        _module.checkForWarning()
    }

    companion object
}

class MobileAppRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    MobileRunConfigurationBase(project, factory, name) {

    override fun isSuitable(module: Module): Boolean = module.isMobileAppMain

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        MobileRunConfigurationEditor(project, ::isSuitable)

    override fun createOtherState(environment: ExecutionEnvironment): CommandLineState {
        return AndroidAppCommandLineState(this, environment)
    }
}