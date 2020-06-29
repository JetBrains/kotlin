package com.jetbrains.swift.codeinsight.resolve

import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration

class MobileSwiftSourceModuleProvider : SwiftSourceModuleProvider {
    override fun isAvailable(configuration: OCResolveConfiguration): Boolean = Companion.isAvailable(configuration)

    override fun createModule(configuration: OCResolveConfiguration): SwiftModule = MobileSwiftSourceModule(configuration)

    companion object {
        fun isAvailable(configuration: OCResolveConfiguration): Boolean =
            GradleAppleWorkspace.getInstance(configuration.project).isOwnerOf(configuration)
                    && configuration.sourceUrls.any { FileUtilRt.extensionEquals(it, "swift") }
    }
}