package com.jetbrains.cidr.apple.bridging

import com.jetbrains.cidr.apple.gradle.AppleTargetModel
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.swift.codeinsight.resolve.SwiftModule
import com.jetbrains.swift.codeinsight.resolve.SwiftModuleManager
import com.jetbrains.swift.symbols.SwiftBridgeTarget

data class MobileBridgeTarget(val targetModel: AppleTargetModel) : SwiftBridgeTarget {
    override fun getTargetModule(configuration: OCResolveConfiguration): SwiftModule? {
        val project = configuration.project
        val derivedConfiguration = GradleAppleWorkspace.getInstance(project).getConfiguration(targetModel.name) ?: return null
        return SwiftModuleManager.getInstance(project).getSourceModule(derivedConfiguration)
    }
}