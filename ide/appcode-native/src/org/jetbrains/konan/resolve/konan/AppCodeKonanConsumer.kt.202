package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import org.jetbrains.konan.gradle.execution.AppCodeGradleKonanExternalBuildProvider
import org.jetbrains.konan.gradle.execution.filterGradleTasks

class AppCodeKonanConsumer : KonanConsumer {
    override fun getReferencedKonanTargets(project: Project): List<KonanTarget> =
        XcodeMetaData.getInstance(project).allProjects.asSequence()
            .flatMap { it.getTargets(PBXTarget::class.java).asSequence() }
            .filterGradleTasks(AppCodeGradleKonanExternalBuildProvider.GRADLE_BUILD_TASK_NAME, project)
            .map { AppCodeKonanTarget(it) }
            .toList()
}