package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.gradle.KonanArtifactModel

interface KonanTargetManager {
    fun forArtifact(moduleId: String, artifact: KonanArtifactModel): KonanTarget?

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KonanTargetManager = project.service()
    }
}