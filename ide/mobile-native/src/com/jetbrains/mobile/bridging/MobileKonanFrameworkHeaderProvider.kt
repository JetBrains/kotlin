package com.jetbrains.mobile.bridging

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.apple.bridging.MobileKonanTarget
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import com.jetbrains.cidr.lang.CustomHeaderProvider
import com.jetbrains.cidr.lang.OCIncludeHelpers.adjustHeaderName
import com.jetbrains.cidr.lang.preprocessor.OCResolveRootAndConfiguration
import org.jetbrains.konan.resolve.konan.KonanBridgeFileManager
import org.jetbrains.konan.resolve.konan.KonanBridgeVirtualFile

class MobileKonanFrameworkHeaderProvider : CustomHeaderProvider() {
    init {
        registerProvider(HeaderSearchStage.BEFORE_LIBRARIES) { headerName, configuration ->
            configuration?.project?.let { getHeader(it, headerName) }
        }
    }

    override fun accepts(resolveRootAndConfiguration: OCResolveRootAndConfiguration?): Boolean {
        resolveRootAndConfiguration ?: return false
        val config = resolveRootAndConfiguration.configuration ?: return false

        val target = GradleAppleWorkspace.getInstance(config.project).getTarget(config) ?: return false
        return true

        TODO("check for dependencies")
        /*val targets = mutableListOf(target)

        //targets.addAll(frameworkDependencies(target))
        return targets.asSequence().containsKotlinNativeTargets(config.project)*/
    }

    override fun provideSerializationPath(virtualFile: VirtualFile): String? {
        if (!virtualFile.isValid || virtualFile !is KonanBridgeVirtualFile) return null

        val target = virtualFile.target
        if (target !is MobileKonanTarget) return null

        return "$prefix::${target.moduleId}::${target.productModuleName}"
    }

    override fun getCustomSerializedHeaderFile(serializationPath: String, project: Project, currentFile: VirtualFile): VirtualFile? {
        val items = serializationPath.split("::", limit = 4)
        if (items.size != 3 || prefix != items[0]) return null

        val moduleId = items[1]
        val productModuleName = items[2]
        return KonanBridgeFileManager.getInstance(project)
            .forTarget(MobileKonanTarget(moduleId, productModuleName), "$productModuleName/$productModuleName.h")
    }

    private fun getHeader(project: Project, import: String): VirtualFile? {
        val adjustedName = adjustHeaderName(import)

        val parts: List<String> = adjustedName.split("/", limit = 3)
        if (parts.size != 2) return null

        val frameworkName = parts[0]
        val headerName = StringUtil.trimEnd(parts[1], ".h")
        if (!FileUtil.pathsEqual(frameworkName, headerName)) return null

        val konanTarget = GradleAppleWorkspace.getInstance(project).availableKonanFrameworkTargets[frameworkName]
        return konanTarget?.let { KonanBridgeFileManager.getInstance(project).forTarget(it, adjustedName) }
    }

    companion object {
        private const val prefix = "konan-bridge"
    }
}
