package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.xcode.model.PBXNativeTarget
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import org.jetbrains.kotlin.gradle.KonanArtifactModel

class AppCodeKonanTargetManager(private val project: Project) : KonanTargetManager {
    override fun forArtifact(moduleId: String, artifact: KonanArtifactModel): AppCodeKonanTarget? {
        for (target in XcodeMetaData.getInstance(project).getTargets(PBXNativeTarget::class.java)) {
            if (target.isFramework && FileUtil.pathsEqual(target.productReference?.path, artifact.file.name))
                return AppCodeKonanTarget(target)
        }
        return null
    }
}