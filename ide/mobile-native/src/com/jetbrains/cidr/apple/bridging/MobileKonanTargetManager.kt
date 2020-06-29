package com.jetbrains.cidr.apple.bridging

import com.intellij.openapi.project.Project
import org.jetbrains.konan.resolve.konan.KonanTargetManager
import org.jetbrains.kotlin.gradle.KonanArtifactModel

class MobileKonanTargetManager : KonanTargetManager {
    override fun forArtifact(moduleId: String, artifact: KonanArtifactModel): MobileKonanTarget =
        MobileKonanTarget(moduleId, artifact.file.nameWithoutExtension)

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MobileKonanTargetManager = KonanTargetManager.getInstance(project) as MobileKonanTargetManager
    }
}