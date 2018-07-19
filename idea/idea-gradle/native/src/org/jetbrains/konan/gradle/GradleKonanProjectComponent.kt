package org.jetbrains.konan.gradle

import com.intellij.openapi.project.Project
import org.jetbrains.konan.settings.KonanProjectComponent
import org.jetbrains.plugins.gradle.settings.GradleSettings

class GradleKonanProjectComponent(project: Project) : KonanProjectComponent(project) {
  override fun looksLikeKotlinNativeProject(): Boolean {
    //TODO not just any gradle project
    return GradleSettings.getInstance(project).linkedProjectsSettings.isNotEmpty()
  }
}