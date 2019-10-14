package com.jetbrains.swift.sourcekitd

import com.intellij.openapi.util.Version
import com.jetbrains.cidr.xcode.Xcode
import java.io.File

class MobileSourceKitServiceManager : SourceKitServiceManager() {
    companion object {
        private val DEFAULT_TOOLCHAIN_PATH = Xcode.getApplicationContentsSubFile("Developer/Toolchains/XcodeDefault.xctoolchain")
    }

    override fun getValidatedToolchainPath(version: Version): File? {
        if (version == DEFAULT_VERSION) return DEFAULT_TOOLCHAIN_PATH

        val versionString = Version.toCompactString(version.major, version.minor, 0)
        return Xcode.getApplicationContentsSubFile("Developer/Toolchains/Swift_$versionString.xctoolchain")
    }
}