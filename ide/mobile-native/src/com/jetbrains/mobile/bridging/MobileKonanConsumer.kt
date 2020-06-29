package com.jetbrains.mobile.bridging

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import org.jetbrains.konan.resolve.konan.KonanConsumer
import org.jetbrains.konan.resolve.konan.KonanTarget

class MobileKonanConsumer : KonanConsumer {
    // todo[florian.kistner] restrict to declared dependencies per apple target
    override fun getReferencedKonanTargets(project: Project): Collection<KonanTarget> =
        GradleAppleWorkspace.getInstance(project).availableKonanFrameworkTargets.values
}