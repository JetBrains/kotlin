package org.jetbrains.konan

import com.intellij.openapi.project.Project
import org.jetbrains.konan.gradle.forEachKonanProject
import org.jetbrains.kotlin.gradle.KonanArtifactModel

inline fun forEachKonanFrameworkTarget(project: Project, consumer: (moduleId: String, artifact: KonanArtifactModel) -> Unit) {
    forEachKonanProject(project) { konanModel, module, _ ->
        for (artifact in konanModel.artifacts) {
            if (artifact.type == "FRAMEWORK") consumer(module.data.id, artifact)
        }
    }
}

