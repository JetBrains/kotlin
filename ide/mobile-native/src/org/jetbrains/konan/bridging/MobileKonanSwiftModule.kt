package org.jetbrains.konan.bridging

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.apple.bridging.MobileKonanTarget
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import org.jetbrains.konan.resolve.konan.KonanBridgeFileManager
import org.jetbrains.konan.resolve.konan.KonanBridgeVirtualFile
import org.jetbrains.konan.resolve.konan.KonanSwiftModule

class MobileKonanSwiftModule(
    private val target: MobileKonanTarget,
    private val parentConfiguration: OCResolveConfiguration
) : KonanSwiftModule() {

    override val project: Project
        get() = parentConfiguration.project

    override fun getName(): String = target.productModuleName

    override fun getConfiguration(): OCResolveConfiguration = parentConfiguration

    override fun konanBridgeFile(): KonanBridgeVirtualFile =
        KonanBridgeFileManager.getInstance(project).forTarget(target, name.replace('-', '_').let { "$it/$it.h" })

    override fun getFiles(): List<VirtualFile> = emptyList()
}
