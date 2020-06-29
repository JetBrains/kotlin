package com.jetbrains.swift

import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.Xcode
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.cidr.xcode.frameworks.buildSystem.ArchitectureValue

class MobileSwiftCompilerSettings : SwiftCompilerSettings() {
    override fun getSwiftToolchainPath(): String? = Xcode.getSwiftToolchainPath()

    override fun isPlatform(configuration: OCResolveConfiguration, platform: String): Boolean {
        val applePlatform = AppleSdkManager.getInstance().findPlatformByType(ApplePlatform.Type.IOS_SIMULATOR)!!
        return when (platform) {
            "OSX", "macOS" -> applePlatform.isMacOS
            "iOS" -> applePlatform.isIOS
            "watchOS" -> applePlatform.isWatch
            "tvOS" -> applePlatform.isTv
            else -> false
        }
    }

    override fun isArchitecture(configuration: OCResolveConfiguration, architecture: String): Boolean =
        architecture == ArchitectureValue.x86_64.id
}
