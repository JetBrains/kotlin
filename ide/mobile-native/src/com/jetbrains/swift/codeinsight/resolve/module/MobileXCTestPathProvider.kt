package com.jetbrains.swift.codeinsight.resolve.module

import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.Xcode

class MobileXCTestPathProvider : SwiftCustomIncludePathProvider {
    override fun getCustomLibrarySearchPaths(configuration: OCResolveConfiguration): List<String> =
        if (Xcode.isInstalled())
            listOf(
                Xcode.getSubFilePath("Platforms/iPhoneSimulator.platform/Developer/Library/Frameworks")
            )
        else emptyList()
}