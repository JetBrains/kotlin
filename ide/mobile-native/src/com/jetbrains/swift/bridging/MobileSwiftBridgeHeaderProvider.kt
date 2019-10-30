package com.jetbrains.swift.bridging

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.apple.bridging.MobileBridgeTarget
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import com.jetbrains.cidr.lang.CustomHeaderProvider
import com.jetbrains.cidr.lang.OCIncludeHelpers
import com.jetbrains.cidr.lang.preprocessor.OCResolveRootAndConfiguration
import com.jetbrains.swift.codeinsight.resolve.MobileSwiftSourceModuleProvider
import com.jetbrains.swift.codeinsight.resolve.SwiftModuleManager
import com.jetbrains.swift.symbols.SwiftBridgeVirtualFile

class MobileSwiftBridgeHeaderProvider : CustomHeaderProvider() {
    init {
        registerProvider(HeaderSearchStage.BEFORE_LIBRARIES) { headerName, configuration ->
            val config = configuration ?: return@registerProvider null
            SwiftModuleManager.getInstance(config.project)
                .getSourceModule(config)
                ?.getBridgeFile(OCIncludeHelpers.adjustHeaderName(headerName))
        }

        registerProvider(HeaderSearchStage.AFTER_END) { headerName, configuration ->
            val config = configuration ?: return@registerProvider null

            val name = OCIncludeHelpers.adjustHeaderName(headerName)
            val parts = name.split("/", limit = 3)
            if (parts.size != 2) return@registerProvider null

            val moduleName = parts[0]
            if (moduleName.isEmpty()) return@registerProvider null

            SwiftModuleManager.getInstance(config.project)
                .getModule(config, moduleName, true)
                ?.getBridgeFile(name)
        }
    }

    override fun accepts(configuration: OCResolveRootAndConfiguration?): Boolean {
        val config = configuration?.configuration ?: return false
        return MobileSwiftSourceModuleProvider.isAvailable(config)
    }

    override fun provideSerializationPath(virtualFile: VirtualFile): String? {
        if (!virtualFile.isValid || virtualFile !is SwiftBridgeVirtualFile) return null

        val target = virtualFile.target
        if (target !is MobileBridgeTarget) return null

        return "${prefix}::" + target.targetModel.name + "::" + virtualFile.name // TODO Unify with CustomTargetHeaderSerializationHelper?
    }

    override fun getCustomSerializedHeaderFile(serializationPath: String, project: Project, currentFile: VirtualFile): VirtualFile? {
        val items = serializationPath.split("::", limit = 4)
        if (items.size != 3 || prefix != items[0]) return null

        val targetName = items[1]
        val headerName = OCIncludeHelpers.adjustHeaderName(items[2])

        val config = GradleAppleWorkspace.getInstance(project).getConfiguration(project, targetName) ?: return null
        return SwiftModuleManager.getInstance(project).getSourceModule(config)?.getBridgeFile(headerName)
    }

    companion object {
        private const val prefix = "swift-bridge"
    }
}