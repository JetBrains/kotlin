package org.jetbrains.konan

import com.intellij.openapi.project.Project
import org.jetbrains.konan.gradle.forEachKonanProject
import org.jetbrains.konan.resolve.konan.KonanTarget
import org.jetbrains.konan.resolve.konan.KonanTargetManager

fun getKonanFrameworkTargets(project: Project): List<KonanTarget> = mutableListOf<KonanTarget>().apply {
    forEachKonanProject(project) { konanModel, module, _ ->
        for (artifact in konanModel.artifacts) {
            if (artifact.type != "FRAMEWORK") continue
            add(KonanTargetManager.getInstance(project).forArtifact(module.data.id, artifact) ?: continue)
        }
    }
}