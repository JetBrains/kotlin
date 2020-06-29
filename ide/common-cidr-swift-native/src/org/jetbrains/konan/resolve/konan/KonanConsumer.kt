package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface KonanConsumer {
    fun getReferencedKonanTargets(project: Project): Collection<KonanTarget>

    companion object {
        private val EP_NAME = ExtensionPointName.create<KonanConsumer>("org.jetbrains.konan.konanConsumer")

        @JvmStatic
        fun getAllReferencedKonanTargets(project: Project): List<KonanTarget> =
            EP_NAME.extensionList.flatMap { it.getReferencedKonanTargets(project) }
    }
}
