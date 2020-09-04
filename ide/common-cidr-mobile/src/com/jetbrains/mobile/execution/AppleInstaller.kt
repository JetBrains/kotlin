package com.jetbrains.mobile.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.cidr.execution.CidrCommandLineConfigurator
import com.jetbrains.cidr.execution.CidrProgramParameters
import com.jetbrains.cidr.execution.OCCommandLineConfigurator
import com.jetbrains.cidr.execution.OCInstaller
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.cidr.xcode.frameworks.buildSystem.ArchitectureValue
import java.io.File

open class AppleInstaller(
    private val configuration: MobileRunConfiguration,
    private val device: AppleDevice,
    environment: ExecutionEnvironment,
    appBundle: File
) : OCInstaller(environment, appBundle, appBundle, false) {
    override fun doInstall(): GeneralCommandLine {
        val commandLine = device.install(bundle, project)

        val params = CidrProgramParameters().also {
            it.workingDirectory = File(commandLine.exePath).parentFile.parent
            it.isPassParentEnvs = false
        }

        val platform = AppleSdkManager.getInstance().findPlatformByType(device.platformType)!!
        createCommandLineBuilder(params, platform, device.arch).configureCommandLine(commandLine)
        return commandLine
    }

    protected open fun createCommandLineBuilder(
        params: CidrProgramParameters,
        platform: ApplePlatform,
        arch: ArchitectureValue
    ): CidrCommandLineConfigurator = OCCommandLineConfigurator(project, params, platform, arch, null, false)

    override fun getRunConfiguration(): MobileRunConfiguration = configuration
}