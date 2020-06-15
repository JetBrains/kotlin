package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import com.jetbrains.swift.bridging.SwiftBridgingUtil
import com.jetbrains.swift.lang.parser.SwiftFileType

class AppCodeKonanSourceModule(
    private val configuration: OCResolveConfiguration,
    private val target: PBXTarget
) : KonanSwiftModule() {

    override val project: Project get() = configuration.project

    override fun getName(): String = SwiftBridgingUtil.getProductModuleName(configuration)

    override fun getConfiguration(): OCResolveConfiguration = configuration

    override fun konanBridgeFile(): KonanBridgeVirtualFile? {
        val buildConfig = XcodeMetaData.getBuildConfigurationFor(configuration) ?: return null
        val target = buildConfig.target ?: return null
        // todo replace with something like
        // KonanBridgeFileManager.getInstance(project).forTarget(target, name.replace('-', '_').let { "$it/$it.h" })
        return KonanBridgeVirtualFile(AppCodeKonanTarget(target), name, project, 0)
    }

    override fun getFiles(): List<VirtualFile> = getSourceAndDerivedFiles(target) ?: target.sourceFiles

    private fun getSourceAndDerivedFiles(target: PBXTarget): List<VirtualFile>? =
        XcodeMetaData.getBuildSettings(configuration)?.getSourceAndDerivedFiles(target, SwiftFileType.INSTANCE)
}
